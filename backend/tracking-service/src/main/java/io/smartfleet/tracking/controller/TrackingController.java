package io.smartfleet.tracking.controller;

import io.smartfleet.common.dto.ApiResponse;
import io.smartfleet.common.dto.LocationDto;
import io.smartfleet.common.dto.VehicleStateDto;
import io.smartfleet.common.exception.ResourceNotFoundException;
import io.smartfleet.tracking.service.TrackingService;
import io.smartfleet.tracking.sse.LocationSseEmitterRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for tracking and telemetry endpoints.
 */
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;
    private final LocationSseEmitterRegistry sseEmitterRegistry;

    @PostMapping("/telemetry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> ingestTelemetry(@Valid @RequestBody LocationDto locationDto) {
        trackingService.ingestTelemetry(locationDto);
        // Broadcast to SSE subscribers
        sseEmitterRegistry.broadcastToAll(locationDto);
        return ApiResponse.of(null);
    }

    /**
     * Endpoint for driver app to send location updates.
     * Automatically associates the location with the authenticated driver.
     */
    @PostMapping("/location")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> updateDriverLocation(
            @Valid @RequestBody DriverLocationUpdate locationUpdate,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID driverId = UUID.fromString(jwt.getSubject());
        Long vehicleId = jwt.hasClaim("vehicle_id") 
                ? jwt.getClaim("vehicle_id") 
                : null;

        LocationDto locationDto = LocationDto.builder()
                .driverId(driverId)
                .vehicleId(vehicleId)
                .latitude(locationUpdate.latitude())
                .longitude(locationUpdate.longitude())
                .heading(locationUpdate.heading())
                .speedKmh(locationUpdate.speedKmh())
                .timestamp(locationUpdate.timestamp() != null 
                        ? locationUpdate.timestamp() 
                        : OffsetDateTime.now())
                .build();

        trackingService.ingestTelemetry(locationDto);
        sseEmitterRegistry.broadcastToAll(locationDto);
        
        return ApiResponse.of(null);
    }

    @GetMapping("/vehicles/{vehicleId}/state")
    public ApiResponse<VehicleStateDto> getVehicleState(@PathVariable Long vehicleId) {
        VehicleStateDto state = trackingService.getVehicleState(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleState", vehicleId));
        return ApiResponse.of(state);
    }

    @GetMapping("/vehicles/{vehicleId}/history")
    public ApiResponse<List<LocationDto>> getLocationHistory(
            @PathVariable Long vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        List<LocationDto> history = trackingService.getLocationHistory(vehicleId, startTime, endTime);
        return ApiResponse.of(history, 1, history.size(), history.size());
    }

    @GetMapping("/trips/{tripId}/locations")
    public ApiResponse<List<LocationDto>> getTripLocations(@PathVariable Long tripId) {
        List<LocationDto> locations = trackingService.getTripLocations(tripId);
        return ApiResponse.of(locations, 1, locations.size(), locations.size());
    }

    /**
     * DTO for driver location updates from mobile app.
     */
    public record DriverLocationUpdate(
            Double latitude,
            Double longitude,
            Double heading,
            Double speedKmh,
            OffsetDateTime timestamp
    ) {}
}
