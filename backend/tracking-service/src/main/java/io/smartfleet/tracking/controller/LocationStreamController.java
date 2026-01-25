package io.smartfleet.tracking.controller;

import io.smartfleet.common.dto.LocationDto;
import io.smartfleet.tracking.sse.LocationSseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
public class LocationStreamController {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes
    
    private final LocationSseEmitterRegistry emitterRegistry;

    /**
     * SSE endpoint for receiving all driver location updates
     */
    @GetMapping(value = "/locations", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAllLocations() {
        log.info("New SSE connection for all locations");
        SseEmitter emitter = emitterRegistry.createEmitter(SSE_TIMEOUT);
        
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\",\"message\":\"Streaming all driver locations\"}"));
        } catch (Exception e) {
            log.error("Failed to send initial SSE event", e);
        }
        
        return emitter;
    }

    /**
     * SSE endpoint for receiving location updates for a specific vehicle
     */
    @GetMapping(value = "/locations/{vehicleId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamVehicleLocation(@PathVariable UUID vehicleId) {
        log.info("New SSE connection for vehicle: {}", vehicleId);
        SseEmitter emitter = emitterRegistry.createEmitter(SSE_TIMEOUT);
        String emitterId = emitterRegistry.registerEmitter(emitter);
        emitterRegistry.subscribeToVehicle(emitterId, vehicleId);
        
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(String.format("{\"status\":\"connected\",\"vehicleId\":\"%s\"}", vehicleId)));
        } catch (Exception e) {
            log.error("Failed to send initial SSE event for vehicle {}", vehicleId, e);
        }
        
        return emitter;
    }

    /**
     * SSE endpoint for receiving location updates for multiple vehicles
     */
    @PostMapping(value = "/locations/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToVehicles(@RequestBody SubscriptionRequest request) {
        log.info("New SSE subscription for {} vehicles", request.vehicleIds().size());
        SseEmitter emitter = emitterRegistry.createEmitter(SSE_TIMEOUT);
        String emitterId = emitterRegistry.registerEmitter(emitter);
        
        for (UUID vehicleId : request.vehicleIds()) {
            emitterRegistry.subscribeToVehicle(emitterId, vehicleId);
        }
        
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(String.format("{\"status\":\"connected\",\"subscribedCount\":%d}", 
                            request.vehicleIds().size())));
        } catch (Exception e) {
            log.error("Failed to send initial SSE event", e);
        }
        
        return emitter;
    }

    /**
     * Get active SSE connection count (for monitoring)
     */
    @GetMapping("/stats")
    public StreamStats getStreamStats() {
        return new StreamStats(emitterRegistry.getActiveEmitterCount());
    }

    public record SubscriptionRequest(java.util.List<UUID> vehicleIds) {}
    
    public record StreamStats(int activeConnections) {}
}
