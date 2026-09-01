package com.auction.auction.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuctionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuctionScheduler.class);

    private final AuctionService auctionService;

    public AuctionScheduler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Scheduled(fixedDelay = 30000) // check every 30 seconds
    public void processExpiredAuctions() {
        log.debug("Running scheduled check for expired auctions...");
        auctionService.checkAndCloseExpiredAuctions();
    }
}
