package io.smartfleet.payment.config;

import io.smartfleet.common.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Payment Service.
 */
@Configuration
public class PaymentServiceConfig {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
