package io.smartfleet.geo.provider;

import io.smartfleet.geo.dto.RouteInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

/**
 * Mock implementation of RoutingProvider for development and testing.
 * Uses Haversine formula for distance calculation and estimates travel time.
 */
@Component
@ConditionalOnProperty(name = "geo.provider", havingValue = "mock")
@Slf4j
public class MockRoutingProvider implements RoutingProvider {

    private static final double EARTH_RADIUS_METERS = 6371000;
    private static final double AVERAGE_SPEED_KMH = 30.0; // Urban driving speed

    @Override
    public RouteInfo calculateRoute(BigDecimal originLat, BigDecimal originLon,
                                     BigDecimal destLat, BigDecimal destLon) {
        log.debug("MOCK: Calculating route from ({}, {}) to ({}, {})",
                originLat, originLon, destLat, destLon);

        double distanceMeters = calculateDistance(originLat, originLon, destLat, destLon);

        // Estimate travel time based on average speed
        double durationSeconds = (distanceMeters / 1000.0) / AVERAGE_SPEED_KMH * 3600.0;

        RouteInfo route = RouteInfo.builder()
                .distanceMeters(BigDecimal.valueOf(distanceMeters).setScale(2, RoundingMode.HALF_UP))
                .duration(Duration.ofSeconds((long) durationSeconds))
                .waypoints(List.of(
                        RouteInfo.Waypoint.builder().latitude(originLat).longitude(originLon).build(),
                        RouteInfo.Waypoint.builder().latitude(destLat).longitude(destLon).build()
                ))
                .polyline(null) // No polyline in mock mode
                .build();

        log.info("MOCK: Route calculated - distance: {}m, duration: {}s",
                route.getDistanceMeters(), route.getDuration().toSeconds());

        return route;
    }

    @Override
    public double calculateDistance(BigDecimal lat1, BigDecimal lon1,
                                     BigDecimal lat2, BigDecimal lon2) {
        return haversineDistance(
                lat1.doubleValue(), lon1.doubleValue(),
                lat2.doubleValue(), lon2.doubleValue()
        );
    }

    /**
     * Haversine formula for calculating distance between two points on Earth.
     *
     * @return Distance in meters
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}
