package com.auction.payment.client;

import com.auction.payment.exception.InvalidPaymentException;
import com.auction.payment.exception.ResourceNotFoundException;
import com.auction.payment.exception.ServiceUnavailableException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PaymentFeignErrorDecoderTest {

    private PaymentFeignErrorDecoder errorDecoder;

    @BeforeEach
    void setUp() {
        errorDecoder = new PaymentFeignErrorDecoder();
    }

    private Response createMockResponse(int status, String reason) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://auction-service/api/auctions/1",
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
    void testDecode400ToInvalidPaymentException() {
        Response response = createMockResponse(400, "Bad Request");
        Exception ex = errorDecoder.decode("AuctionClient#getAuctionById(Long)", response);
        assertInstanceOf(InvalidPaymentException.class, ex);
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
