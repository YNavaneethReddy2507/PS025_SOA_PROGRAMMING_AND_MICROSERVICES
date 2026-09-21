package com.auction.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalLoggingFilterTest {

    private GlobalLoggingFilter loggingFilter;

    @BeforeEach
    void setUp() {
        loggingFilter = new GlobalLoggingFilter();
    }

    @Test
    @DisplayName("GlobalLoggingFilter executes and forwards exchange through filter chain")
    void testLoggingFilterExecution() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auctions/1").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        loggingFilter.filter(exchange, chain).block();

        assertTrue(chainInvoked.get(), "Chain should be called by GlobalLoggingFilter");
        assertEquals(org.springframework.core.Ordered.HIGHEST_PRECEDENCE, loggingFilter.getOrder());
    }
}
