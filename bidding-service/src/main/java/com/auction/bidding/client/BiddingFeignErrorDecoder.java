package com.auction.bidding.client;

import com.auction.bidding.exception.InvalidBidException;
import com.auction.bidding.exception.ResourceNotFoundException;
import com.auction.bidding.exception.ServiceUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BiddingFeignErrorDecoder implements ErrorDecoder {

    private static final Logger log = LoggerFactory.getLogger(BiddingFeignErrorDecoder.class);
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.warn("Feign error occurred for method [{}]: HTTP status {}", methodKey, response.status());

        if (response.status() == 404) {
            return new ResourceNotFoundException("Target resource not found in downstream service (" + methodKey + ")");
        }
        if (response.status() == 400) {
            return new InvalidBidException("Downstream auction service rejected request with 400 Bad Request");
        }
        if (response.status() >= 500) {
            return new ServiceUnavailableException("Downstream auction service is temporarily unavailable (status " + response.status() + ")");
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
