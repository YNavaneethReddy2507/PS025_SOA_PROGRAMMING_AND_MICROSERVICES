package com.auction.auction.client;

import com.auction.auction.dto.PaymentSettlementRequest;
import com.auction.auction.dto.PaymentSettlementResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    @PostMapping("/api/v1/payments/settle")
    PaymentSettlementResponse processSettlement(@RequestBody PaymentSettlementRequest request);
}
