package com.auction.auction.client;

import com.auction.auction.exception.InvalidAuctionStateException;
import com.auction.auction.exception.ResourceNotFoundException;
import com.auction.auction.exception.ServiceUnavailableException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AuctionFeignErrorDecoderTest {

    private AuctionFeignErrorDecoder errorDecoder;

    @BeforeEach
    void setUp() {
        errorDecoder = new AuctionFeignErrorDecoder();
    }

    private Response createMockResponse(int status, String reason) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://bidding-service/api/v1/bids/auction/1/clearance",
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
        Exception ex = errorDecoder.decode("BiddingClient#calculateClearance(Long)", response);
        assertInstanceOf(ResourceNotFoundException.class, ex);
    }

    @Test
    void testDecode400ToInvalidAuctionStateException() {
        Response response = createMockResponse(400, "Bad Request");
        Exception ex = errorDecoder.decode("PaymentClient#processSettlement(PaymentSettlementRequest)", response);
        assertInstanceOf(InvalidAuctionStateException.class, ex);
    }

    @Test
    void testDecode503ToServiceUnavailableException() {
        Response response = createMockResponse(503, "Service Unavailable");
        Exception ex = errorDecoder.decode("BiddingClient#calculateClearance(Long)", response);
        assertInstanceOf(ServiceUnavailableException.class, ex);
    }

    @Test
    void testDecode500ToServiceUnavailableException() {
        Response response = createMockResponse(500, "Internal Server Error");
        Exception ex = errorDecoder.decode("PaymentClient#processSettlement(PaymentSettlementRequest)", response);
        assertInstanceOf(ServiceUnavailableException.class, ex);
    }
}
