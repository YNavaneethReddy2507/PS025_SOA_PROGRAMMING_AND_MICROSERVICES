package com.auction.bidding.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_bid_states")
public class AuctionBidState {

    @Id
    @Column(name = "auction_id")
    private Long auctionId;

    @Column(name = "current_highest_bid", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentHighestBid;

    @Column(name = "winning_bidder_id")
    private Long winningBidderId;

    @Column(name = "min_bid_increment", nullable = false, precision = 19, scale = 4)
    private BigDecimal minBidIncrement;

    @Column(name = "reserve_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservePrice;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "total_bids_count", nullable = false)
    private Long totalBidsCount;

    @Version
    private Long version;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AuctionBidState() {
    }

    public AuctionBidState(Long auctionId, BigDecimal currentHighestBid, Long winningBidderId,
                           BigDecimal minBidIncrement, BigDecimal reservePrice, Long sellerId,
                           Long totalBidsCount, Long version, LocalDateTime updatedAt) {
        this.auctionId = auctionId;
        this.currentHighestBid = currentHighestBid;
        this.winningBidderId = winningBidderId;
        this.minBidIncrement = minBidIncrement;
        this.reservePrice = reservePrice;
        this.sellerId = sellerId;
        this.totalBidsCount = totalBidsCount;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public static AuctionBidStateBuilder builder() {
        return new AuctionBidStateBuilder();
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public BigDecimal getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(BigDecimal currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public Long getWinningBidderId() {
        return winningBidderId;
    }

    public void setWinningBidderId(Long winningBidderId) {
        this.winningBidderId = winningBidderId;
    }

    public BigDecimal getMinBidIncrement() {
        return minBidIncrement;
    }

    public void setMinBidIncrement(BigDecimal minBidIncrement) {
        this.minBidIncrement = minBidIncrement;
    }

    public BigDecimal getReservePrice() {
        return reservePrice;
    }

    public void setReservePrice(BigDecimal reservePrice) {
        this.reservePrice = reservePrice;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Long getTotalBidsCount() {
        return totalBidsCount;
    }

    public void setTotalBidsCount(Long totalBidsCount) {
        this.totalBidsCount = totalBidsCount;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class AuctionBidStateBuilder {
        private Long auctionId;
        private BigDecimal currentHighestBid;
        private Long winningBidderId;
        private BigDecimal minBidIncrement;
        private BigDecimal reservePrice;
        private Long sellerId;
        private Long totalBidsCount;
        private Long version;
        private LocalDateTime updatedAt;

        AuctionBidStateBuilder() {
        }

        public AuctionBidStateBuilder auctionId(Long auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public AuctionBidStateBuilder currentHighestBid(BigDecimal currentHighestBid) {
            this.currentHighestBid = currentHighestBid;
            return this;
        }

        public AuctionBidStateBuilder winningBidderId(Long winningBidderId) {
            this.winningBidderId = winningBidderId;
            return this;
        }

        public AuctionBidStateBuilder minBidIncrement(BigDecimal minBidIncrement) {
            this.minBidIncrement = minBidIncrement;
            return this;
        }

        public AuctionBidStateBuilder reservePrice(BigDecimal reservePrice) {
            this.reservePrice = reservePrice;
            return this;
        }

        public AuctionBidStateBuilder sellerId(Long sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public AuctionBidStateBuilder totalBidsCount(Long totalBidsCount) {
            this.totalBidsCount = totalBidsCount;
            return this;
        }

        public AuctionBidStateBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public AuctionBidStateBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public AuctionBidState build() {
            return new AuctionBidState(auctionId, currentHighestBid, winningBidderId, minBidIncrement, reservePrice, sellerId, totalBidsCount, version, updatedAt);
        }
    }
}
