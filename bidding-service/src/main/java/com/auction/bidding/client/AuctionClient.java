package com.auction.bidding.client;

import com.auction.bidding.dto.AuctionDto;
import com.auction.bidding.dto.UpdateHighestBidRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auction-service")
public interface AuctionClient {

    @GetMapping("/api/v1/auctions/{id}")
    AuctionDto getAuctionById(@PathVariable("id") Long id);

    @PutMapping("/api/v1/auctions/{id}/highest-bid")
    AuctionDto updateHighestBid(@PathVariable("id") Long id, @RequestBody UpdateHighestBidRequest request);
}
