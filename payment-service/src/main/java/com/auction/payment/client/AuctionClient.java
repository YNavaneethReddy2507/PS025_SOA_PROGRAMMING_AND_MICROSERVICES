package com.auction.payment.client;

import com.auction.payment.dto.AuctionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auction-service")
public interface AuctionClient {

    @GetMapping("/api/auctions/{id}")
    AuctionDto getAuctionById(@PathVariable("id") Long id);
}
