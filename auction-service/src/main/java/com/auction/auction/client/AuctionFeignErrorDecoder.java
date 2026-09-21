package com.auction.auction.client;

import com.auction.auction.exception.InvalidAuctionStateException;
import com.auction.auction.exception.ResourceNotFoundException;
import com.auction.auction.exception.ServiceUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuctionFeignErrorDecoder implements ErrorDecoder {

    private static final Logger log = LoggerFactory.getLogger(AuctionFeignErrorDecoder.class);
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.warn("Feign error in Auction Service for method [{}]: HTTP status {}", methodKey, response.status());

        if (response.status() == 404) {
            return new ResourceNotFoundException("Resource not found in downstream service (" + methodKey + ")");
        }
        if (response.status() == 400) {
            return new InvalidAuctionStateException("Downstream service rejected request with 400 Bad Request");
        }
        if (response.status() >= 500) {
            return new ServiceUnavailableException("Downstream service is temporarily unavailable (status " + response.status() + ")");
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
