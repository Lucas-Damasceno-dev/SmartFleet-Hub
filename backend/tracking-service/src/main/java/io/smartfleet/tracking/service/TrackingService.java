package io.smartfleet.tracking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smartfleet.common.dto.LocationDto;
import io.smartfleet.common.dto.VehicleStateDto;
import io.smartfleet.tracking.entity.Location;
import io.smartfleet.tracking.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service implementing CQRS pattern for GPS telemetry.
 * Write Path: TimescaleDB (cold storage) + Redis (hot storage)
 * Read Path: Redis for live data, TimescaleDB for history
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private static final String VEHICLE_STATE_KEY_PREFIX = "vehicle:";
    private static final String VEHICLE_STATE_KEY_SUFFIX = ":state";
    private static final Duration VEHICLE_STATE_TTL = Duration.ofHours(1);
    
    // Geofencing constants
    private static final double ARRIVAL_RADIUS_METERS = 200.0;
    private static final String ORDER_DESTINATION_KEY_PREFIX = "order:";
    private static final String ORDER_DESTINATION_KEY_SUFFIX = ":destination";
    private static final String GEOFENCE_EVENTS_TOPIC = "geofence-events";

    private final LocationRepository locationRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Ingest telemetry data using CQRS pattern.
     * Writes to both TimescaleDB (cold) and Redis (hot).
     * Also checks for geofence triggers.
     */
    @Transactional
    public void ingestTelemetry(LocationDto locationDto) {
        log.debug("Ingesting telemetry for vehicle {}", locationDto.getVehicleId());

        // Write to TimescaleDB (Cold Storage - History)
        Location location = toEntity(locationDto);
        locationRepository.save(location);

        // Update Redis (Hot Storage - Live Data)
        updateVehicleState(locationDto);

        // Check geofencing triggers
        checkGeofenceTriggers(locationDto);

        log.debug("Telemetry ingested for vehicle {}", locationDto.getVehicleId());
    }

    /**
     * Batch ingest telemetry data for high-throughput scenarios.
     * Uses saveAll for optimized batch insert to TimescaleDB.
     */
    @Transactional
    public void ingestTelemetryBatch(List<LocationDto> locationDtos) {
        if (locationDtos.isEmpty()) {
            return;
        }

        log.info("Batch ingesting {} telemetry records", locationDtos.size());

        // Convert to entities
        List<Location> locations = locationDtos.stream()
                .map(this::toEntity)
                .toList();

        // Batch insert to TimescaleDB
        locationRepository.saveAll(locations);

        // Update Redis for each vehicle (latest state only)
        locationDtos.forEach(dto -> {
            updateVehicleState(dto);
            checkGeofenceTriggers(dto);
        });

        log.info("Batch ingestion complete for {} records", locationDtos.size());
    }

    /**
     * Check if vehicle has entered a geofence (pickup zone).
     * Emits DRIVER_NEARBY event if within 200m of destination.
     */
    private void checkGeofenceTriggers(LocationDto locationDto) {
        if (locationDto.getTripId() == null) {
            return; // No active trip, skip geofence check
        }

        // Get order destination from Redis
        String destinationKey = ORDER_DESTINATION_KEY_PREFIX + locationDto.getTripId() + ORDER_DESTINATION_KEY_SUFFIX;
        String destinationJson = redisTemplate.opsForValue().get(destinationKey);

        if (destinationJson == null) {
            return; // No destination registered
        }

        try {
            DestinationPoint destination = objectMapper.readValue(destinationJson, DestinationPoint.class);
            
            double distance = haversineDistance(
                    locationDto.getLatitude().doubleValue(),
                    locationDto.getLongitude().doubleValue(),
                    destination.latitude(),
                    destination.longitude()
            );

            if (distance <= ARRIVAL_RADIUS_METERS) {
                log.info("GEOFENCE TRIGGER: Vehicle {} entered pickup zone for order {} (distance: {}m)",
                        locationDto.getVehicleId(), locationDto.getTripId(), distance);
                
                emitDriverNearbyEvent(locationDto.getVehicleId(), locationDto.getTripId(), distance);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse destination for trip {}", locationDto.getTripId(), e);
        }
    }

    /**
     * Emit DRIVER_NEARBY event to Kafka.
     */
    private void emitDriverNearbyEvent(Long vehicleId, Long orderId, double distance) {
        try {
            DriverNearbyEvent event = new DriverNearbyEvent(
                    orderId,
                    vehicleId,
                    distance,
                    OffsetDateTime.now().toString()
            );
            
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(GEOFENCE_EVENTS_TOPIC, orderId.toString(), eventJson);
            
            log.info("Emitted DRIVER_NEARBY event for order {}", orderId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DRIVER_NEARBY event for order {}", orderId, e);
        }
    }

    /**
     * Register a destination for geofence monitoring.
     */
    public void registerOrderDestination(Long orderId, BigDecimal latitude, BigDecimal longitude) {
        String key = ORDER_DESTINATION_KEY_PREFIX + orderId + ORDER_DESTINATION_KEY_SUFFIX;
        DestinationPoint destination = new DestinationPoint(latitude.doubleValue(), longitude.doubleValue());
        
        try {
            String json = objectMapper.writeValueAsString(destination);
            redisTemplate.opsForValue().set(key, json, Duration.ofHours(24));
            log.info("Registered destination for order {} at ({}, {})", orderId, latitude, longitude);
        } catch (JsonProcessingException e) {
            log.error("Failed to register destination for order {}", orderId, e);
        }
    }

    /**
     * Get current vehicle state from Redis (Hot Storage).
     */
    public Optional<VehicleStateDto> getVehicleState(Long vehicleId) {
        String key = buildVehicleStateKey(vehicleId);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return Optional.empty();
        }

        try {
            VehicleStateDto state = objectMapper.readValue(json, VehicleStateDto.class);
            return Optional.of(state);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize vehicle state for {}", vehicleId, e);
            return Optional.empty();
        }
    }

    /**
     * Get location history from TimescaleDB (Cold Storage).
     */
    @Transactional(readOnly = true)
    public List<LocationDto> getLocationHistory(Long vehicleId, OffsetDateTime startTime, OffsetDateTime endTime) {
        return locationRepository.findByVehicleIdAndTimeRange(vehicleId, startTime, endTime)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get trip locations from TimescaleDB.
     */
    @Transactional(readOnly = true)
    public List<LocationDto> getTripLocations(Long tripId) {
        return locationRepository.findByTripId(tripId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void updateVehicleState(LocationDto locationDto) {
        VehicleStateDto state = VehicleStateDto.builder()
                .vehicleId(locationDto.getVehicleId())
                .latitude(locationDto.getLatitude())
                .longitude(locationDto.getLongitude())
                .speedKmh(locationDto.getSpeedKmh())
                .status(determineStatus(locationDto))
                .ignitionOn(locationDto.getIgnitionOn())
                .currentTripId(locationDto.getTripId())
                .lastUpdate(locationDto.getTime())
                .build();

        try {
            String json = objectMapper.writeValueAsString(state);
            String key = buildVehicleStateKey(locationDto.getVehicleId());
            redisTemplate.opsForValue().set(key, json, VEHICLE_STATE_TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize vehicle state for {}", locationDto.getVehicleId(), e);
        }
    }

    private String determineStatus(LocationDto locationDto) {
        if (locationDto.getIgnitionOn() == null || !locationDto.getIgnitionOn()) {
            return "STOPPED";
        }
        if (locationDto.getSpeedKmh() == null || locationDto.getSpeedKmh().doubleValue() < 5) {
            return "IDLE";
        }
        return "MOVING";
    }

    private String buildVehicleStateKey(Long vehicleId) {
        return VEHICLE_STATE_KEY_PREFIX + vehicleId + VEHICLE_STATE_KEY_SUFFIX;
    }

    /**
     * Haversine formula for distance calculation in meters.
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusMeters = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusMeters * c;
    }

    private Location toEntity(LocationDto dto) {
        return Location.builder()
                .time(dto.getTime())
                .vehicleId(dto.getVehicleId())
                .tripId(dto.getTripId())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .speedKmh(dto.getSpeedKmh())
                .headingDegrees(dto.getHeadingDegrees())
                .batteryLevel(dto.getBatteryLevel())
                .ignitionOn(dto.getIgnitionOn())
                .accuracyMeters(dto.getAccuracyMeters())
                .build();
    }

    private LocationDto toDto(Location entity) {
        return LocationDto.builder()
                .time(entity.getTime())
                .vehicleId(entity.getVehicleId())
                .tripId(entity.getTripId())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .speedKmh(entity.getSpeedKmh())
                .headingDegrees(entity.getHeadingDegrees())
                .batteryLevel(entity.getBatteryLevel())
                .ignitionOn(entity.getIgnitionOn())
                .accuracyMeters(entity.getAccuracyMeters())
                .build();
    }

    // Inner records for geofencing
    private record DestinationPoint(double latitude, double longitude) {}
    private record DriverNearbyEvent(Long orderId, Long vehicleId, double distanceMeters, String timestamp) {}
}
