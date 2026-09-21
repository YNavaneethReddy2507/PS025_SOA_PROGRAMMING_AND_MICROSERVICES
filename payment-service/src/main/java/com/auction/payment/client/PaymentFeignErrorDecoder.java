package com.auction.payment.client;

import com.auction.payment.exception.InvalidPaymentException;
import com.auction.payment.exception.ResourceNotFoundException;
import com.auction.payment.exception.ServiceUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PaymentFeignErrorDecoder implements ErrorDecoder {

    private static final Logger log = LoggerFactory.getLogger(PaymentFeignErrorDecoder.class);
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.warn("Feign error in Payment Service for method [{}]: HTTP status {}", methodKey, response.status());

        if (response.status() == 404) {
            return new ResourceNotFoundException("Resource not found in auction service (" + methodKey + ")");
        }
        if (response.status() == 400) {
            return new InvalidPaymentException("Auction service rejected request with 400 Bad Request");
        }
        if (response.status() >= 500) {
            return new ServiceUnavailableException("Auction service is temporarily unavailable (status " + response.status() + ")");
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
