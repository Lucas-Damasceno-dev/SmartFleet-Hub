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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final LocationRepository locationRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Ingest telemetry data using CQRS pattern.
     * Writes to both TimescaleDB (cold) and Redis (hot).
     */
    @Transactional
    public void ingestTelemetry(LocationDto locationDto) {
        log.debug("Ingesting telemetry for vehicle {}", locationDto.getVehicleId());

        // Write to TimescaleDB (Cold Storage - History)
        Location location = toEntity(locationDto);
        locationRepository.save(location);

        // Update Redis (Hot Storage - Live Data)
        updateVehicleState(locationDto);

        log.debug("Telemetry ingested for vehicle {}", locationDto.getVehicleId());
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
}
