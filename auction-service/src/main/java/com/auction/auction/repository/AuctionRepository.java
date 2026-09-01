package com.auction.auction.repository;

import com.auction.auction.entity.Auction;
import com.auction.auction.entity.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    List<Auction> findByStatus(AuctionStatus status);

    List<Auction> findByCategory(String category);

    List<Auction> findByStatusAndCategory(AuctionStatus status, String category);

    List<Auction> findBySellerId(Long sellerId);

    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.endTime <= :cutoff")
    List<Auction> findExpiredAuctions(
            @Param("status") AuctionStatus status,
            @Param("cutoff") LocalDateTime cutoff
    );
}
