package com.auction.auction.client;

import com.auction.auction.dto.ClearanceResultDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "bidding-service")
public interface BiddingClient {

    @GetMapping("/api/v1/bids/auction/{auctionId}/clearance")
    ClearanceResultDto calculateClearance(@PathVariable("auctionId") Long auctionId);
}
