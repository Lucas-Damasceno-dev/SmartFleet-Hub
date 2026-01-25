package io.smartfleet.geo.provider;

import io.smartfleet.geo.dto.RouteInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Google Maps Platform implementation of RoutingProvider.
 * Uses Directions API for route calculation.
 */
@Component
@ConditionalOnProperty(name = "geo.provider", havingValue = "google", matchIfMissing = true)
@Slf4j
public class GoogleMapsRoutingProvider implements RoutingProvider {

    private static final String DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final double EARTH_RADIUS_METERS = 6371000;

    @Value("${geo.google-maps.api-key:}")
    private String apiKey;

    private final RestClient restClient;

    public GoogleMapsRoutingProvider() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    public RouteInfo calculateRoute(BigDecimal originLat, BigDecimal originLon,
                                     BigDecimal destLat, BigDecimal destLon) {
        log.debug("Calculating route from ({}, {}) to ({}, {})",
                originLat, originLon, destLat, destLon);

        if (apiKey == null || apiKey.isBlank() || apiKey.contains("PLACEHOLDER")) {
            log.warn("Google Maps API key not configured, using fallback calculation");
            return calculateFallbackRoute(originLat, originLon, destLat, destLon);
        }

        try {
            String origin = originLat + "," + originLon;
            String destination = destLat + "," + destLon;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(DIRECTIONS_API_URL + "?origin={origin}&destination={destination}&key={key}",
                            origin, destination, apiKey)
                    .retrieve()
                    .body(Map.class);

            return parseDirectionsResponse(response);
        } catch (Exception e) {
            log.error("Failed to call Google Maps API, using fallback", e);
            return calculateFallbackRoute(originLat, originLon, destLat, destLon);
        }
    }

    @Override
    public double calculateDistance(BigDecimal lat1, BigDecimal lon1,
                                    BigDecimal lat2, BigDecimal lon2) {
        return haversineDistance(
                lat1.doubleValue(), lon1.doubleValue(),
                lat2.doubleValue(), lon2.doubleValue()
        );
    }

    private RouteInfo calculateFallbackRoute(BigDecimal originLat, BigDecimal originLon,
                                              BigDecimal destLat, BigDecimal destLon) {
        double distanceMeters = calculateDistance(originLat, originLon, destLat, destLon);
        
        // Assume average speed of 30 km/h for urban driving
        double durationSeconds = (distanceMeters / 1000.0) / 30.0 * 3600.0;

        return RouteInfo.builder()
                .distanceMeters(BigDecimal.valueOf(distanceMeters).setScale(2, RoundingMode.HALF_UP))
                .duration(Duration.ofSeconds((long) durationSeconds))
                .waypoints(List.of(
                        RouteInfo.Waypoint.builder().latitude(originLat).longitude(originLon).build(),
                        RouteInfo.Waypoint.builder().latitude(destLat).longitude(destLon).build()
                ))
                .build();
    }

    @SuppressWarnings("unchecked")
    private RouteInfo parseDirectionsResponse(Map<String, Object> response) {
        if (response == null || !"OK".equals(response.get("status"))) {
            throw new RuntimeException("Invalid Google Maps API response");
        }

        List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
        if (routes == null || routes.isEmpty()) {
            throw new RuntimeException("No routes found");
        }

        Map<String, Object> route = routes.get(0);
        List<Map<String, Object>> legs = (List<Map<String, Object>>) route.get("legs");
        Map<String, Object> leg = legs.get(0);

        Map<String, Object> distance = (Map<String, Object>) leg.get("distance");
        Map<String, Object> duration = (Map<String, Object>) leg.get("duration");

        return RouteInfo.builder()
                .distanceMeters(BigDecimal.valueOf(((Number) distance.get("value")).longValue()))
                .duration(Duration.ofSeconds(((Number) duration.get("value")).longValue()))
                .polyline((String) ((Map<String, Object>) route.get("overview_polyline")).get("points"))
                .build();
    }

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
