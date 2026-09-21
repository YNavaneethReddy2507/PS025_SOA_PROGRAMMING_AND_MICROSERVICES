package com.auction.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    @Autowired
    private GatewayProperties gatewayProperties;

    @Test
    @DisplayName("Verify API Gateway registers all required microservice routes with load balancing")
    void testMicroserviceRoutesConfigured() {
        List<RouteDefinition> routes = gatewayProperties.getRoutes();
        assert routes != null;

        Set<String> routeIds = routes.stream()
                .map(RouteDefinition::getId)
                .collect(Collectors.toSet());

        assertTrue(routeIds.contains("auth-service"), "Route 'auth-service' must be configured");
        assertTrue(routeIds.contains("auction-service"), "Route 'auction-service' must be configured");
        assertTrue(routeIds.contains("bidding-service"), "Route 'bidding-service' must be configured");
        assertTrue(routeIds.contains("payment-service"), "Route 'payment-service' must be configured");

        for (RouteDefinition route : routes) {
            if (Set.of("auth-service", "auction-service", "bidding-service", "payment-service").contains(route.getId())) {
                assertEquals("lb", route.getUri().getScheme(),
                        "Route " + route.getId() + " must use Eureka/LoadBalancer scheme (lb://)");
            }
        }
    }
}
