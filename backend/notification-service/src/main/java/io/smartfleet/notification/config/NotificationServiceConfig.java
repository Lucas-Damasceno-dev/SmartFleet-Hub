package io.smartfleet.notification.config;

import io.smartfleet.common.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Notification Service.
 */
@Configuration
public class NotificationServiceConfig {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
