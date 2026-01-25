package io.smartfleet.tracking.controller;

import io.smartfleet.common.dto.ApiResponse;
import io.smartfleet.common.dto.LocationDto;
import io.smartfleet.common.dto.VehicleStateDto;
import io.smartfleet.common.exception.ResourceNotFoundException;
import io.smartfleet.tracking.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * REST controller for tracking and telemetry endpoints.
 */
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/telemetry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> ingestTelemetry(@Valid @RequestBody LocationDto locationDto) {
        trackingService.ingestTelemetry(locationDto);
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
}
