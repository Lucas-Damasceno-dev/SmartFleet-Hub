package io.smartfleet.order.config;

import io.smartfleet.common.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Order Service.
 */
@Configuration
public class OrderServiceConfig {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
