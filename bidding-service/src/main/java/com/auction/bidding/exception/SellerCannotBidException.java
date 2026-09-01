package com.auction.bidding.exception;

public class SellerCannotBidException extends RuntimeException {
    public SellerCannotBidException(String message) {
        super(message);
    }
}
