package com.auction.bidding.client;

import com.auction.bidding.exception.InvalidBidException;
import com.auction.bidding.exception.ResourceNotFoundException;
import com.auction.bidding.exception.ServiceUnavailableException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BiddingFeignErrorDecoderTest {

    private BiddingFeignErrorDecoder errorDecoder;

    @BeforeEach
    void setUp() {
        errorDecoder = new BiddingFeignErrorDecoder();
    }

    private Response createMockResponse(int status, String reason) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://auction-service/api/v1/auctions/1",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        return Response.builder()
                .status(status)
                .reason(reason)
                .request(request)
                .headers(Collections.emptyMap())
                .build();
    }

    @Test
    void testDecode404ToResourceNotFoundException() {
        Response response = createMockResponse(404, "Not Found");
        Exception ex = errorDecoder.decode("AuctionClient#getAuctionById(Long)", response);
        assertInstanceOf(ResourceNotFoundException.class, ex);
    }

    @Test
    void testDecode400ToInvalidBidException() {
        Response response = createMockResponse(400, "Bad Request");
        Exception ex = errorDecoder.decode("AuctionClient#updateHighestBid(Long,UpdateHighestBidRequest)", response);
        assertInstanceOf(InvalidBidException.class, ex);
    }

    @Test
    void testDecode503ToServiceUnavailableException() {
        Response response = createMockResponse(503, "Service Unavailable");
        Exception ex = errorDecoder.decode("AuctionClient#getAuctionById(Long)", response);
        assertInstanceOf(ServiceUnavailableException.class, ex);
    }

    @Test
    void testDecode500ToServiceUnavailableException() {
        Response response = createMockResponse(500, "Internal Server Error");
        Exception ex = errorDecoder.decode("AuctionClient#getAuctionById(Long)", response);
        assertInstanceOf(ServiceUnavailableException.class, ex);
    }
}
