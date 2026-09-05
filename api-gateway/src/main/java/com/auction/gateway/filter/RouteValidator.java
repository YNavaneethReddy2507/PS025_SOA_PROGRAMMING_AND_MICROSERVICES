package com.auction.gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/validate",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/validate",
            "/eureka",
            "/actuator"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> OPEN_API_ENDPOINTS
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().startsWith(uri));
}
