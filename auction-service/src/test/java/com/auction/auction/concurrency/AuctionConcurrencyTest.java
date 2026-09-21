package com.auction.auction.concurrency;

import com.auction.auction.client.BiddingClient;
import com.auction.auction.client.PaymentClient;
import com.auction.auction.dto.*;
import com.auction.auction.entity.Auction;
import com.auction.auction.entity.AuctionStatus;
import com.auction.auction.exception.InvalidAuctionStateException;
import com.auction.auction.repository.AuctionRepository;
import com.auction.auction.service.AuctionScheduler;
import com.auction.auction.service.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class AuctionConcurrencyTest {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AuctionScheduler auctionScheduler;

    @MockBean
    private BiddingClient biddingClient;

    @MockBean
    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() {
        auctionRepository.deleteAll();
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertNotNull(actual, "Actual BigDecimal must not be null");
        assertEquals(0, expected.compareTo(actual), String.format("Expected %s but was %s", expected, actual));
    }

    private Auction createSampleAuction(Long sellerId, BigDecimal startingPrice, BigDecimal minInc, BigDecimal reservePrice, AuctionStatus status) {
        LocalDateTime now = LocalDateTime.now();
        Auction auction = Auction.builder()
                .title("Rare Collectible")
                .description("High value item")
                .category("COLLECTIBLES")
                .startingPrice(startingPrice)
                .currentPrice(startingPrice)
                .minimumIncrement(minInc)
                .reservePrice(reservePrice)
                .startTime(now.minusHours(1))
                .endTime(now.plusHours(2))
                .sellerId(sellerId)
                .status(status)
                .build();
        return auctionRepository.save(auction);
    }

    @Test
    @DisplayName("Concurrency: Simultaneous bid update and auction close - deterministic resolution")
    void testSimultaneousBidAndClose() throws InterruptedException {
        Auction auction = createSampleAuction(10L, new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("200.00"), AuctionStatus.ACTIVE);
        Long auctionId = auction.getId();

        ClearanceResultDto mockClearance = ClearanceResultDto.builder()
                .auctionId(auctionId)
                .winningBidderId(55L)
                .winningAmount(new BigDecimal("250.00"))
                .reserveMet(true)
                .clearedAt(LocalDateTime.now())
                .clearanceStatus("CLEARED")
                .message("Deterministic clearance")
                .build();

        when(biddingClient.calculateClearance(auctionId)).thenReturn(mockClearance);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger bidSuccess = new AtomicInteger(0);
        AtomicInteger closeSuccess = new AtomicInteger(0);

        // Thread 1: places highest bid
        executor.submit(() -> {
            try {
                startLatch.await();
                UpdateHighestBidRequest req = UpdateHighestBidRequest.builder()
                        .bidderId(55L)
                        .amount(new BigDecimal("250.00"))
                        .build();
                auctionService.updateHighestBid(auctionId, req);
                bidSuccess.incrementAndGet();
            } catch (InvalidAuctionStateException ignored) {
                // Expected if close executed first
            } catch (Exception e) {
                fail("Unexpected error in bid update: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: closes auction
        executor.submit(() -> {
            try {
                startLatch.await();
                auctionService.closeAuction(auctionId, 10L, "ROLE_SELLER");
                closeSuccess.incrementAndGet();
            } catch (Exception e) {
                fail("Unexpected error in close auction: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Timed out waiting for simultaneous bid and close");
        executor.shutdown();

        // Verification:
        // Close MUST succeed in all cases
        assertEquals(1, closeSuccess.get(), "Auction close must succeed");

        Auction closed = auctionRepository.findById(auctionId).orElseThrow();
        assertEquals(AuctionStatus.CLOSED, closed.getStatus(), "Auction must be in CLOSED state");

        // Invariants:
        // 1. winner == deterministic winning bid
        assertEquals(55L, closed.getWinnerId(), "Winner must match deterministic winning bidder");
        // 2. highest accepted bid == current auction price
        assertBigDecimalEquals(new BigDecimal("250.00"), closed.getCurrentPrice());
    }

    @Test
    @DisplayName("Concurrency: Repeated close request across multiple threads - idempotent closing")
    void testRepeatedCloseRequestConcurrently() throws InterruptedException {
        Auction auction = createSampleAuction(20L, new BigDecimal("200.00"), new BigDecimal("20.00"), new BigDecimal("300.00"), AuctionStatus.ACTIVE);
        Long auctionId = auction.getId();

        ClearanceResultDto mockClearance = ClearanceResultDto.builder()
                .auctionId(auctionId)
                .winningBidderId(77L)
                .winningAmount(new BigDecimal("350.00"))
                .reserveMet(true)
                .clearedAt(LocalDateTime.now())
                .clearanceStatus("CLEARED")
                .message("Deterministic clearance")
                .build();

        when(biddingClient.calculateClearance(auctionId)).thenReturn(mockClearance);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<AuctionResponse> responses = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    AuctionResponse res = auctionService.closeAuction(auctionId, 20L, "ROLE_SELLER");
                    responses.add(res);
                } catch (Exception e) {
                    fail("Repeated close should not fail: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Timed out waiting for repeated close requests");
        executor.shutdown();

        // All 10 requests must succeed idempotently
        assertEquals(threadCount, responses.size());
        for (AuctionResponse r : responses) {
            assertEquals(AuctionStatus.CLOSED, r.getStatus());
            assertEquals(77L, r.getWinnerId());
            assertBigDecimalEquals(new BigDecimal("350.00"), r.getCurrentPrice());
        }

        Auction finalAuction = auctionRepository.findById(auctionId).orElseThrow();
        assertEquals(AuctionStatus.CLOSED, finalAuction.getStatus());
        assertEquals(77L, finalAuction.getWinnerId());
        assertBigDecimalEquals(new BigDecimal("350.00"), finalAuction.getCurrentPrice());
    }

    @Test
    @DisplayName("Concurrency: Simultaneous closeAndClearAuction - single payment settlement")
    void testSimultaneousCloseAndClear() throws InterruptedException {
        Auction auction = createSampleAuction(30L, new BigDecimal("500.00"), new BigDecimal("50.00"), new BigDecimal("600.00"), AuctionStatus.ACTIVE);
        Long auctionId = auction.getId();

        ClearanceResultDto mockClearance = ClearanceResultDto.builder()
                .auctionId(auctionId)
                .winningBidderId(99L)
                .winningAmount(new BigDecimal("750.00"))
                .reserveMet(true)
                .clearedAt(LocalDateTime.now())
                .clearanceStatus("CLEARED")
                .message("Cleared successfully")
                .build();

        when(biddingClient.calculateClearance(auctionId)).thenReturn(mockClearance);
        when(paymentClient.processSettlement(any(PaymentSettlementRequest.class))).thenReturn(
                PaymentSettlementResponse.builder()
                        .transactionId("TX-12345")
                        .status("SUCCESS")
                        .build()
        );

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<ClearanceResultDto> results = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ClearanceResultDto res = auctionService.closeAndClearAuction(auctionId);
                    results.add(res);
                } catch (Exception e) {
                    fail("Concurrent close and clear failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly one settlement request was executed; others returned idempotent ALREADY_CLOSED
        verify(paymentClient, atMost(1)).processSettlement(any(PaymentSettlementRequest.class));

        Auction finalAuction = auctionRepository.findById(auctionId).orElseThrow();
        assertEquals(AuctionStatus.CLOSED, finalAuction.getStatus());
        assertEquals(99L, finalAuction.getWinnerId());
        assertBigDecimalEquals(new BigDecimal("750.00"), finalAuction.getCurrentPrice());
    }

    @Test
    @DisplayName("Automatic Auction Closure: Scheduler detects expired auctions and closes them")
    void testAuctionExpirationScheduler() {
        LocalDateTime past = LocalDateTime.now().minusMinutes(10);
        Auction expiredAuction = Auction.builder()
                .title("Antique Clock")
                .description("Antique pendulum clock")
                .category("ANTIQUES")
                .startingPrice(new BigDecimal("300.00"))
                .currentPrice(new BigDecimal("300.00"))
                .minimumIncrement(new BigDecimal("25.00"))
                .reservePrice(new BigDecimal("400.00"))
                .startTime(past.minusHours(2))
                .endTime(past)
                .sellerId(40L)
                .status(AuctionStatus.ACTIVE)
                .build();
        expiredAuction = auctionRepository.save(expiredAuction);

        ClearanceResultDto mockClearance = ClearanceResultDto.builder()
                .auctionId(expiredAuction.getId())
                .winningBidderId(88L)
                .winningAmount(new BigDecimal("450.00"))
                .reserveMet(true)
                .clearedAt(LocalDateTime.now())
                .clearanceStatus("CLEARED")
                .message("Scheduled clearance")
                .build();

        when(biddingClient.calculateClearance(expiredAuction.getId())).thenReturn(mockClearance);
        when(paymentClient.processSettlement(any(PaymentSettlementRequest.class))).thenReturn(
                PaymentSettlementResponse.builder().transactionId("TX-SCHED").status("SUCCESS").build()
        );

        // Run scheduler
        auctionScheduler.processExpiredAuctions();

        // Verify auction transitioned to CLOSED with winner
        Auction closed = auctionRepository.findById(expiredAuction.getId()).orElseThrow();
        assertEquals(AuctionStatus.CLOSED, closed.getStatus());
        assertEquals(88L, closed.getWinnerId());
        assertBigDecimalEquals(new BigDecimal("450.00"), closed.getCurrentPrice());
    }
}
