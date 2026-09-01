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

    List<Bid> findByAuctionIdOrderByBidAmountDescBidTimestampAsc(Long auctionId);

    List<Bid> findByAuctionIdOrderByBidTimestampDesc(Long auctionId);

    Optional<Bid> findFirstByAuctionIdAndStatus(Long auctionId, BidStatus status);

    @Modifying
    @Query("UPDATE Bid b SET b.status = :newStatus WHERE b.auctionId = :auctionId AND b.status = :oldStatus")
    void updateBidStatusForAuction(
            @Param("auctionId") Long auctionId,
            @Param("oldStatus") BidStatus oldStatus,
            @Param("newStatus") BidStatus newStatus
    );

    long countByAuctionId(Long auctionId);
}
