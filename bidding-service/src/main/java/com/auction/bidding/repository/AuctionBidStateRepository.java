package com.auction.bidding.repository;

import com.auction.bidding.entity.AuctionBidState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuctionBidStateRepository extends JpaRepository<AuctionBidState, Long> {
    Optional<AuctionBidState> findByAuctionId(Long auctionId);
}
