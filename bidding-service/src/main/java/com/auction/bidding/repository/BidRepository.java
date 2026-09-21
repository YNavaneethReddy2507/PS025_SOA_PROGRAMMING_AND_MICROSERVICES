package com.auction.bidding.repository;

import com.auction.bidding.entity.Bid;
import com.auction.bidding.entity.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    @Query("SELECT b FROM Bid b WHERE b.auctionId = :auctionId AND b.status = com.auction.bidding.entity.BidStatus.ACCEPTED ORDER BY b.amount DESC, b.acceptedAt ASC, b.id ASC")
    List<Bid> findByAuctionIdOrderByAmountDescAcceptedAtAsc(@Param("auctionId") Long auctionId);

    @Query("SELECT b FROM Bid b WHERE b.auctionId = :auctionId ORDER BY b.acceptedAt DESC")
    List<Bid> findByAuctionIdOrderByAcceptedAtDesc(@Param("auctionId") Long auctionId);

    @Query("SELECT b FROM Bid b WHERE b.auctionId = :auctionId AND b.status = :status ORDER BY b.amount DESC, b.acceptedAt ASC, b.id ASC")
    List<Bid> findByAuctionIdAndStatus(@Param("auctionId") Long auctionId, @Param("status") BidStatus status);

    Optional<Bid> findFirstByAuctionIdAndStatusOrderByAmountDescAcceptedAtAscIdAsc(Long auctionId, BidStatus status);

    default Optional<Bid> findHighestAcceptedBid(Long auctionId) {
        List<Bid> list = findByAuctionIdOrderByAmountDescAcceptedAtAsc(auctionId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    // Compatibility aliases
    default List<Bid> findByAuctionIdOrderByBidAmountDescBidTimestampAsc(Long auctionId) {
        return findByAuctionIdOrderByAmountDescAcceptedAtAsc(auctionId);
    }

    default List<Bid> findByAuctionIdOrderByBidTimestampDesc(Long auctionId) {
        return findByAuctionIdOrderByAcceptedAtDesc(auctionId);
    }

    default Optional<Bid> findFirstByAuctionIdAndStatus(Long auctionId, BidStatus status) {
        List<Bid> bids = findByAuctionIdAndStatus(auctionId, status);
        return bids.isEmpty() ? Optional.empty() : Optional.of(bids.get(0));
    }

    @Modifying
    @Query("UPDATE Bid b SET b.status = :newStatus WHERE b.auctionId = :auctionId AND b.status = :oldStatus")
    void updateBidStatusForAuction(
            @Param("auctionId") Long auctionId,
            @Param("oldStatus") BidStatus oldStatus,
            @Param("newStatus") BidStatus newStatus
    );

    long countByAuctionId(Long auctionId);
}
