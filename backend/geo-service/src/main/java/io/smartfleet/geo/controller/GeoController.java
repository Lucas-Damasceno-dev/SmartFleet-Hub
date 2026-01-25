package io.smartfleet.geo.controller;

import io.smartfleet.common.dto.ApiResponse;
import io.smartfleet.geo.dto.RouteInfo;
import io.smartfleet.geo.entity.DriverLocation;
import io.smartfleet.geo.service.GeofenceService;
import io.smartfleet.geo.service.PostGISGeofenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * REST controller for geofencing and routing endpoints.
 */
@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeofenceService geofenceService;
    private final PostGISGeofenceService postGISGeofenceService;

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

    @GetMapping("/drivers/{driverId}/arrival-check")
    public ApiResponse<ArrivalCheckResponse> checkDriverArrival(
            @PathVariable Long driverId,
            @RequestParam BigDecimal destLat,
            @RequestParam BigDecimal destLon) {
        
        boolean isNearby = postGISGeofenceService.detectDriverArrival(driverId, destLat, destLon);
        Double distance = postGISGeofenceService.getDistanceToPoint(driverId, destLat, destLon).orElse(null);
        
        return ApiResponse.of(ArrivalCheckResponse.builder()
                .isNearby(isNearby)
                .distanceMeters(distance != null ? distance : -1)
                .build());
    }

    @GetMapping("/drivers/nearby")
    public ApiResponse<List<DriverLocationDto>> findNearbyDrivers(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lon,
            @RequestParam(defaultValue = "5") double radiusKm) {
        
        List<DriverLocation> drivers = postGISGeofenceService.findDriversWithinRadius(lat, lon, radiusKm);
        
        List<DriverLocationDto> dtos = drivers.stream()
                .map(d -> DriverLocationDto.builder()
                        .driverId(d.getDriverId())
                        .vehicleId(d.getVehicleId())
                        .latitude(d.getLocation().getY())
                        .longitude(d.getLocation().getX())
                        .status(d.getStatus())
                        .speedKmh(d.getSpeedKmh())
                        .lastUpdated(d.getLastUpdated())
                        .build())
                .toList();
        
        return ApiResponse.of(dtos);
    }

    @PutMapping("/drivers/{driverId}/location")
    public ApiResponse<DriverLocationDto> updateDriverLocation(
            @PathVariable Long driverId,
            @RequestParam Long vehicleId,
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lon,
            @RequestParam(required = false) Double speedKmh,
            @RequestParam(required = false) Double heading,
            @RequestParam(required = false, defaultValue = "AVAILABLE") String status) {
        
        DriverLocation updated = postGISGeofenceService.updateDriverLocation(
                driverId, vehicleId, lat, lon, speedKmh, heading, status);
        
        return ApiResponse.of(DriverLocationDto.builder()
                .driverId(updated.getDriverId())
                .vehicleId(updated.getVehicleId())
                .latitude(updated.getLocation().getY())
                .longitude(updated.getLocation().getX())
                .status(updated.getStatus())
                .speedKmh(updated.getSpeedKmh())
                .lastUpdated(updated.getLastUpdated())
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

    @lombok.Data
    @lombok.Builder
    public static class DriverLocationDto {
        private Long driverId;
        private Long vehicleId;
        private double latitude;
        private double longitude;
        private String status;
        private Double speedKmh;
        private OffsetDateTime lastUpdated;
    }
}
