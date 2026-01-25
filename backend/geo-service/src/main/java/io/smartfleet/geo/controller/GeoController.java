package io.smartfleet.geo.controller;

import io.smartfleet.common.dto.ApiResponse;
import io.smartfleet.geo.dto.RouteInfo;
import io.smartfleet.geo.service.GeofenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * REST controller for geofencing and routing endpoints.
 */
@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeofenceService geofenceService;

    @GetMapping("/arrival-check")
    public ApiResponse<ArrivalCheckResponse> checkArrival(
            @RequestParam BigDecimal driverLat,
            @RequestParam BigDecimal driverLon,
            @RequestParam BigDecimal destLat,
            @RequestParam BigDecimal destLon) {
        
        boolean isNearby = geofenceService.detectArrival(driverLat, driverLon, destLat, destLon);
        double distance = geofenceService.getDistance(driverLat, driverLon, destLat, destLon);
        
        return ApiResponse.of(ArrivalCheckResponse.builder()
                .isNearby(isNearby)
                .distanceMeters(distance)
                .build());
    }

    @GetMapping("/eta")
    public ApiResponse<EtaResponse> calculateEta(
            @RequestParam BigDecimal currentLat,
            @RequestParam BigDecimal currentLon,
            @RequestParam BigDecimal destLat,
            @RequestParam BigDecimal destLon) {
        
        OffsetDateTime eta = geofenceService.calculateEta(currentLat, currentLon, destLat, destLon);
        RouteInfo route = geofenceService.getRoute(currentLat, currentLon, destLat, destLon);
        
        return ApiResponse.of(EtaResponse.builder()
                .eta(eta)
                .durationMinutes(route.getDuration().toMinutes())
                .distanceKm(route.getDistanceMeters().divide(BigDecimal.valueOf(1000), 2, java.math.RoundingMode.HALF_UP))
                .build());
    }

    @GetMapping("/route")
    public ApiResponse<RouteInfo> getRoute(
            @RequestParam BigDecimal originLat,
            @RequestParam BigDecimal originLon,
            @RequestParam BigDecimal destLat,
            @RequestParam BigDecimal destLon) {
        
        RouteInfo route = geofenceService.getRoute(originLat, originLon, destLat, destLon);
        return ApiResponse.of(route);
    }

    @GetMapping("/distance")
    public ApiResponse<DistanceResponse> getDistance(
            @RequestParam BigDecimal lat1,
            @RequestParam BigDecimal lon1,
            @RequestParam BigDecimal lat2,
            @RequestParam BigDecimal lon2) {
        
        double distance = geofenceService.getDistance(lat1, lon1, lat2, lon2);
        return ApiResponse.of(DistanceResponse.builder()
                .distanceMeters(distance)
                .distanceKm(distance / 1000.0)
                .build());
    }

    @GetMapping("/within-radius")
    public ApiResponse<Boolean> isWithinRadius(
            @RequestParam BigDecimal pointLat,
            @RequestParam BigDecimal pointLon,
            @RequestParam BigDecimal targetLat,
            @RequestParam BigDecimal targetLon,
            @RequestParam double radiusMeters) {
        
        boolean isWithin = geofenceService.isWithinDistance(pointLat, pointLon, targetLat, targetLon, radiusMeters);
        return ApiResponse.of(isWithin);
    }

    // Response DTOs
    @lombok.Data
    @lombok.Builder
    public static class ArrivalCheckResponse {
        private boolean isNearby;
        private double distanceMeters;
    }

    @lombok.Data
    @lombok.Builder
    public static class EtaResponse {
        private OffsetDateTime eta;
        private long durationMinutes;
        private BigDecimal distanceKm;
    }

    @lombok.Data
    @lombok.Builder
    public static class DistanceResponse {
        private double distanceMeters;
        private double distanceKm;
    }
}
