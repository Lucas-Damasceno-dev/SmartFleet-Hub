package io.smartfleet.geo.config;

import io.smartfleet.common.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Geo Service.
 */
@Configuration
public class GeoServiceConfig {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
