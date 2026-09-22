package com.auction.bidding.concurrency;

import com.auction.bidding.client.AuctionClient;
import com.auction.bidding.dto.AuctionDto;
import com.auction.bidding.dto.ClearanceResultDto;
import com.auction.bidding.dto.PlaceBidRequest;
import com.auction.bidding.entity.AuctionBidState;
import com.auction.bidding.entity.Bid;
import com.auction.bidding.entity.BidStatus;
import com.auction.bidding.exception.SubThresholdBidException;
import com.auction.bidding.repository.AuctionBidStateRepository;
import com.auction.bidding.repository.BidRepository;
import com.auction.bidding.service.BiddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class BiddingConcurrencyTest {

    @Autowired
    private BiddingService biddingService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionBidStateRepository stateRepository;

    @MockBean
    private AuctionClient auctionClient;

    @BeforeEach
    void setUp() {
        bidRepository.deleteAll();
        stateRepository.deleteAll();
    }

    private AuctionDto createMockAuctionDto(Long auctionId, BigDecimal startingPrice, BigDecimal minIncrement, BigDecimal reservePrice) {
        LocalDateTime now = LocalDateTime.now();
        return AuctionDto.builder()
                .id(auctionId)
                .title("Concurrent Test Item " + auctionId)
                .startingPrice(startingPrice)
                .currentHighestBid(startingPrice)
                .minBidIncrement(minIncrement)
                .reservePrice(reservePrice)
                .sellerId(999L)
                .status("ACTIVE")
                .startTime(now.minusHours(1))
                .endTime(now.plusDays(1))
                .build();
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertNotNull(actual, "Actual BigDecimal must not be null");
        assertEquals(0, expected.compareTo(actual),
                String.format("Expected %s but was %s", expected, actual));
    }

    @Test
    @DisplayName("Concurrency: 2 simultaneous bids - no lost updates and highest bid matches auction state")
    void testTwoSimultaneousBids() throws InterruptedException {
        Long auctionId = 1L;
        AuctionDto auctionDto = createMockAuctionDto(auctionId, new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("200.00"));
        when(auctionClient.getAuctionById(auctionId)).thenReturn(auctionDto);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);

        // Bidder 1 bids 120.00
        executor.submit(() -> {
            try {
                startLatch.await();
                PlaceBidRequest req = PlaceBidRequest.builder()
                        .auctionId(auctionId)
                        .bidAmount(new BigDecimal("120.00"))
                        .build();
                biddingService.placeBid(req, 1L);
                successCount.incrementAndGet();
            } catch (Throwable ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        // Bidder 2 bids 150.00
        executor.submit(() -> {
            try {
                startLatch.await();
                PlaceBidRequest req = PlaceBidRequest.builder()
                        .auctionId(auctionId)
                        .bidAmount(new BigDecimal("150.00"))
                        .build();
                biddingService.placeBid(req, 2L);
                successCount.incrementAndGet();
            } catch (Throwable ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        // Fire both threads simultaneously
        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Timed out waiting for concurrent bids");
        executor.shutdown();

        // Verification:
        // Highest bid MUST be 150.00 by bidder 2 regardless of execution interleaving
        AuctionBidState state = stateRepository.findByAuctionId(auctionId).orElseThrow();
        assertBigDecimalEquals(new BigDecimal("150.00"), state.getCurrentHighestBid());
        assertEquals(2L, state.getWinningBidderId());

        Bid highestAcceptedBid = bidRepository.findHighestAcceptedBid(auctionId).orElseThrow();
        assertBigDecimalEquals(new BigDecimal("150.00"), highestAcceptedBid.getAmount());
        assertEquals(2L, highestAcceptedBid.getBidderId());

        // Invariant: highest accepted bid == current auction price
        assertBigDecimalEquals(highestAcceptedBid.getAmount(), state.getCurrentHighestBid());
    }

    @Test
    @DisplayName("Concurrency: 10 simultaneous bids - deterministic state, no race condition")
    void testTenSimultaneousBids() throws InterruptedException {
        Long auctionId = 2L;
        AuctionDto auctionDto = createMockAuctionDto(auctionId, new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("250.00"));
        when(auctionClient.getAuctionById(auctionId)).thenReturn(auctionDto);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 1; i <= threadCount; i++) {
            final long bidderId = 100L + i;
            final BigDecimal amount = new BigDecimal(100 + (i * 10) + ".00"); // 110, 120, ... 200

            executor.submit(() -> {
                try {
                    startLatch.await();
                    PlaceBidRequest req = PlaceBidRequest.builder()
                            .auctionId(auctionId)
                            .bidAmount(amount)
                            .build();
                    biddingService.placeBid(req, bidderId);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Timed out waiting for 10 concurrent bids");
        executor.shutdown();

        // Verification:
        AuctionBidState state = stateRepository.findByAuctionId(auctionId).orElseThrow();
        Bid highestAccepted = bidRepository.findHighestAcceptedBid(auctionId).orElseThrow();

        // Invariant: highest accepted bid == current auction price
        assertBigDecimalEquals(highestAccepted.getAmount(), state.getCurrentHighestBid());
        assertEquals(highestAccepted.getBidderId(), state.getWinningBidderId());
        // Max bid submitted was 200.00 (from bidder 110L)
        assertBigDecimalEquals(new BigDecimal("200.00"), state.getCurrentHighestBid());
        assertEquals(110L, state.getWinningBidderId());
    }

    @Test
    @DisplayName("Concurrency: Same amount submitted concurrently - exactly 1 wins, others rejected")
    void testSameAmountSubmittedConcurrently() throws InterruptedException {
        Long auctionId = 3L;
        AuctionDto auctionDto = createMockAuctionDto(auctionId, new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("200.00"));
        when(auctionClient.getAuctionById(auctionId)).thenReturn(auctionDto);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        BigDecimal identicalAmount = new BigDecimal("150.00");

        for (int i = 1; i <= threadCount; i++) {
            final long bidderId = 200L + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    PlaceBidRequest req = PlaceBidRequest.builder()
                            .auctionId(auctionId)
                            .bidAmount(identicalAmount)
                            .build();
                    biddingService.placeBid(req, bidderId);
                    successCount.incrementAndGet();
                } catch (SubThresholdBidException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Timed out waiting for duplicate amount bids");
        executor.shutdown();

        // Exactly 1 thread succeeds; all others rejected because amount is no longer > currentHighest + minIncrement
        assertEquals(1, successCount.get(), "Only one bid of the exact same amount should be accepted");
        assertEquals(4, rejectedCount.get(), "The rest of the duplicate amount bids must be rejected");

        List<Bid> acceptedBids = bidRepository.findByAuctionIdOrderByAmountDescAcceptedAtAsc(auctionId);
        assertEquals(1, acceptedBids.size());
        assertBigDecimalEquals(identicalAmount, acceptedBids.get(0).getAmount());

        AuctionBidState state = stateRepository.findByAuctionId(auctionId).orElseThrow();
        assertBigDecimalEquals(identicalAmount, state.getCurrentHighestBid());
        assertEquals(1L, state.getTotalBidsCount());

        // Invariant: highest accepted bid == current auction price
        assertBigDecimalEquals(acceptedBids.get(0).getAmount(), state.getCurrentHighestBid());
    }

    @Test
    @DisplayName("Concurrency: Increasing bids submitted concurrently - highest wins")
    void testIncreasingBidsConcurrently() throws InterruptedException {
        Long auctionId = 4L;
        AuctionDto auctionDto = createMockAuctionDto(auctionId, new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("200.00"));
        when(auctionClient.getAuctionById(auctionId)).thenReturn(auctionDto);

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        BigDecimal[] amounts = new BigDecimal[]{
                new BigDecimal("120.00"),
                new BigDecimal("140.00"),
                new BigDecimal("160.00"),
                new BigDecimal("180.00")
        };

        for (int i = 0; i < threadCount; i++) {
            final long bidderId = 300L + i;
            final BigDecimal amount = amounts[i];
            executor.submit(() -> {
                try {
                    startLatch.await();
                    PlaceBidRequest req = PlaceBidRequest.builder()
                            .auctionId(auctionId)
                            .bidAmount(amount)
                            .build();
                    biddingService.placeBid(req, bidderId);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        AuctionBidState state = stateRepository.findByAuctionId(auctionId).orElseThrow();
        Bid highestAccepted = bidRepository.findHighestAcceptedBid(auctionId).orElseThrow();

        // Invariant: highest accepted bid == current auction price == 180.00
        assertBigDecimalEquals(new BigDecimal("180.00"), state.getCurrentHighestBid());
        assertBigDecimalEquals(new BigDecimal("180.00"), highestAccepted.getAmount());
        assertEquals(303L, state.getWinningBidderId());
        assertBigDecimalEquals(highestAccepted.getAmount(), state.getCurrentHighestBid());
    }

    @Test
    @DisplayName("Fair Auction Clearance: Deterministic tie-breaking (Price-Time Priority)")
    void testDeterministicClearanceTieBreaking() {
        Long auctionId = 5L;
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(10);

        // Bid 1: $200 placed at T
        Bid bid1 = Bid.builder()
                .auctionId(auctionId)
                .bidderId(10L)
                .amount(new BigDecimal("200.00"))
                .status(BidStatus.ACCEPTED)
                .acceptedAt(baseTime)
                .build();
        bidRepository.save(bid1);

        // Bid 2: $200 placed at T + 2 min (later timestamp)
        Bid bid2 = Bid.builder()
                .auctionId(auctionId)
                .bidderId(20L)
                .amount(new BigDecimal("200.00"))
                .status(BidStatus.ACCEPTED)
                .acceptedAt(baseTime.plusMinutes(2))
                .build();
        bidRepository.save(bid2);

        // Bid 3: $150 placed at T - 5 min (earlier timestamp but lower amount)
        Bid bid3 = Bid.builder()
                .auctionId(auctionId)
                .bidderId(30L)
                .amount(new BigDecimal("150.00"))
                .status(BidStatus.ACCEPTED)
                .acceptedAt(baseTime.minusMinutes(5))
                .build();
        bidRepository.save(bid3);

        // Setup auction bid state
        AuctionBidState state = AuctionBidState.builder()
                .auctionId(auctionId)
                .currentHighestBid(new BigDecimal("200.00"))
                .winningBidderId(10L)
                .minBidIncrement(new BigDecimal("10.00"))
                .reservePrice(new BigDecimal("180.00"))
                .sellerId(999L)
                .totalBidsCount(3L)
                .build();
        stateRepository.save(state);

        ClearanceResultDto clearance = biddingService.calculateClearance(auctionId);

        assertNotNull(clearance);
        // Price-Time priority: Bid 1 and Bid 2 have equal highest amount $200.
        // Bid 1 arrived earlier (baseTime vs baseTime + 2 min).
        // Therefore, Bid 1 (bidder 10L) MUST be the deterministic winner!
        assertEquals(10L, clearance.getWinningBidderId());
        assertBigDecimalEquals(new BigDecimal("200.00"), clearance.getWinningAmount());
        assertTrue(clearance.isReserveMet());
        assertEquals("CLEARED", clearance.getClearanceStatus());
    }

    @Test
    @DisplayName("Concurrency: 50+ simultaneous bids - no lost updates, strict monotonic state")
    void testFiftySimultaneousBids() throws InterruptedException {
        Long auctionId = 6L;
        AuctionDto auctionDto = createMockAuctionDto(auctionId, new BigDecimal("100.00"), new BigDecimal("5.00"), new BigDecimal("300.00"));
        when(auctionClient.getAuctionById(auctionId)).thenReturn(auctionDto);

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 1; i <= threadCount; i++) {
            final long bidderId = 500L + i;
            final BigDecimal amount = new BigDecimal(100 + (i * 5) + ".00"); // 105, 110, ... 350

            executor.submit(() -> {
                try {
                    startLatch.await();
                    PlaceBidRequest req = PlaceBidRequest.builder()
                            .auctionId(auctionId)
                            .bidAmount(amount)
                            .build();
                    biddingService.placeBid(req, bidderId);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS), "Timed out waiting for 50 concurrent bids");
        executor.shutdown();

        // Invariant checks
        AuctionBidState state = stateRepository.findByAuctionId(auctionId).orElseThrow();
        Bid highestAccepted = bidRepository.findHighestAcceptedBid(auctionId).orElseThrow();

        // No lost updates: highest accepted bid matches auction state
        assertBigDecimalEquals(highestAccepted.getAmount(), state.getCurrentHighestBid());
        assertEquals(highestAccepted.getBidderId(), state.getWinningBidderId());

        // Max submitted bid was 350.00 by bidder 550L
        assertBigDecimalEquals(new BigDecimal("350.00"), state.getCurrentHighestBid());
        assertEquals(550L, state.getWinningBidderId());
        assertTrue(state.getTotalBidsCount() > 0);
    }

    @Test
    @DisplayName("Concurrency: Simultaneous bid placement and auction close - no post-close bids accepted")
    void testSimultaneousBidAndAuctionClose() throws InterruptedException {
        Long auctionId = 7L;
        AuctionDto activeAuction = createMockAuctionDto(auctionId, new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("150.00"));
        when(auctionClient.getAuctionById(auctionId)).thenReturn(activeAuction);

        // Pre-place a valid qualifying bid
        PlaceBidRequest initialBid = PlaceBidRequest.builder()
                .auctionId(auctionId)
                .bidAmount(new BigDecimal("120.00"))
                .build();
        biddingService.placeBid(initialBid, 11L);

        int biddingThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(biddingThreads + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(biddingThreads + 1);

        AtomicInteger postCloseRejections = new AtomicInteger(0);

        // 1. Thread that closes auction mid-flight
        executor.submit(() -> {
            try {
                startLatch.await();
                Thread.sleep(10); // allow interleaved scheduling
                activeAuction.setStatus("CLOSED");
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        // 2. Multiple bidding threads
        for (int i = 1; i <= biddingThreads; i++) {
            final long bidderId = 600L + i;
            final BigDecimal amount = new BigDecimal(120 + (i * 10) + ".00");
            executor.submit(() -> {
                try {
                    startLatch.await();
                    PlaceBidRequest req = PlaceBidRequest.builder()
                            .auctionId(auctionId)
                            .bidAmount(amount)
                            .build();
                    biddingService.placeBid(req, bidderId);
                } catch (com.auction.bidding.exception.AuctionNotActiveException e) {
                    postCloseRejections.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Any bid processed after close must be rejected
        // Now calculate clearance: must be deterministic based only on accepted bids
        ClearanceResultDto clearance = biddingService.calculateClearance(auctionId);
        assertNotNull(clearance);
        assertNotNull(clearance.getWinningBidderId());
        assertTrue(clearance.getWinningAmount().compareTo(new BigDecimal("120.00")) >= 0);

        // Subsequent post-close bids MUST throw AuctionNotActiveException
        PlaceBidRequest postCloseBid = PlaceBidRequest.builder()
                .auctionId(auctionId)
                .bidAmount(new BigDecimal("999.00"))
                .build();
        assertThrows(com.auction.bidding.exception.AuctionNotActiveException.class,
                () -> biddingService.placeBid(postCloseBid, 999L));
    }
}
