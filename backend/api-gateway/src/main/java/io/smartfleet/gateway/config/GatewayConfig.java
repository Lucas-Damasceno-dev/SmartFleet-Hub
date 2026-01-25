package io.smartfleet.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .addRequestHeader("X-Gateway-Source", "smartfleet-gateway")
                        )
                        .uri("http://auth-service:8081"))

                // Order Service
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .addRequestHeader("X-Gateway-Source", "smartfleet-gateway")
                        )
                        .uri("http://order-service:8082"))

                // Tracking Service
                .route("tracking-service", r -> r
                        .path("/api/tracking/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .addRequestHeader("X-Gateway-Source", "smartfleet-gateway")
                        )
                        .uri("http://tracking-service:8083"))

                // Tracking SSE Stream
                .route("tracking-stream", r -> r
                        .path("/api/stream/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .addRequestHeader("X-Gateway-Source", "smartfleet-gateway")
                        )
                        .uri("http://tracking-service:8083"))

                // Geo Service
                .route("geo-service", r -> r
                        .path("/api/geo/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .addRequestHeader("X-Gateway-Source", "smartfleet-gateway")
                        )
                        .uri("http://geo-service:8084"))

                // Payment Service
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .addRequestHeader("X-Gateway-Source", "smartfleet-gateway")
                        )
                        .uri("http://payment-service:8085"))

                // Notification Service
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .addRequestHeader("X-Gateway-Source", "smartfleet-gateway")
                        )
                        .uri("http://notification-service:8086"))

                .build();
    }
}
