package io.smartfleet.tracking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smartfleet.common.dto.LocationDto;
import io.smartfleet.tracking.entity.Location;
import io.smartfleet.tracking.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TrackingService geofencing trigger logic.
 */
@ExtendWith(MockitoExtension.class)
class TrackingServiceGeofenceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Captor
    private ArgumentCaptor<String> kafkaTopicCaptor;

    @Captor
    private ArgumentCaptor<String> kafkaKeyCaptor;

    @Captor
    private ArgumentCaptor<String> kafkaValueCaptor;

    private TrackingService trackingService;
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    // Pickup location
    private static final double PICKUP_LAT = -23.5629;
    private static final double PICKUP_LON = -46.6544;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        trackingService = new TrackingService(locationRepository, redisTemplate, objectMapper, kafkaTemplate);
    }

    @Nested
    @DisplayName("Geofence Trigger Tests")
    class GeofenceTriggerTests {

        @Test
        @DisplayName("should emit DRIVER_NEARBY event when driver enters 200m zone")
        void shouldEmitDriverNearbyEventWhenWithin200m() throws Exception {
            // Given: Order destination registered in Redis
            Long tripId = 100L;
            String destinationKey = "order:" + tripId + ":destination";
            String destinationJson = objectMapper.writeValueAsString(
                    new DestinationPoint(PICKUP_LAT, PICKUP_LON)
            );
            when(valueOperations.get(destinationKey)).thenReturn(destinationJson);

            // Driver location within 100m of destination
            LocationDto locationDto = LocationDto.builder()
                    .time(OffsetDateTime.now())
                    .vehicleId(101L)
                    .tripId(tripId)
                    .latitude(BigDecimal.valueOf(PICKUP_LAT + 0.0009)) // ~100m north
                    .longitude(BigDecimal.valueOf(PICKUP_LON))
                    .speedKmh(BigDecimal.valueOf(20))
                    .ignitionOn(true)
                    .build();

            // When
            trackingService.ingestTelemetry(locationDto);

            // Then: Should emit DRIVER_NEARBY event to Kafka
            verify(kafkaTemplate).send(
                    kafkaTopicCaptor.capture(),
                    kafkaKeyCaptor.capture(),
                    kafkaValueCaptor.capture()
            );

            assertThat(kafkaTopicCaptor.getValue()).isEqualTo("geofence-events");
            assertThat(kafkaKeyCaptor.getValue()).isEqualTo(tripId.toString());

            String eventJson = kafkaValueCaptor.getValue();
            assertThat(eventJson).contains("\"orderId\":" + tripId);
            assertThat(eventJson).contains("\"vehicleId\":101");
        }

        @Test
        @DisplayName("should NOT emit event when driver is more than 200m away")
        void shouldNotEmitEventWhenDriverFarAway() throws Exception {
            // Given: Order destination registered in Redis
            Long tripId = 100L;
            String destinationKey = "order:" + tripId + ":destination";
            String destinationJson = objectMapper.writeValueAsString(
                    new DestinationPoint(PICKUP_LAT, PICKUP_LON)
            );
            when(valueOperations.get(destinationKey)).thenReturn(destinationJson);

            // Driver location 500m from destination (outside geofence)
            LocationDto locationDto = LocationDto.builder()
                    .time(OffsetDateTime.now())
                    .vehicleId(101L)
                    .tripId(tripId)
                    .latitude(BigDecimal.valueOf(PICKUP_LAT + 0.0045)) // ~500m north
                    .longitude(BigDecimal.valueOf(PICKUP_LON))
                    .speedKmh(BigDecimal.valueOf(30))
                    .ignitionOn(true)
                    .build();

            // When
            trackingService.ingestTelemetry(locationDto);

            // Then: Should NOT emit any Kafka event
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should not check geofence when no active trip")
        void shouldNotCheckGeofenceWhenNoTrip() {
            // Given: No tripId (driver not on delivery)
            LocationDto locationDto = LocationDto.builder()
                    .time(OffsetDateTime.now())
                    .vehicleId(101L)
                    .tripId(null) // No trip
                    .latitude(BigDecimal.valueOf(PICKUP_LAT))
                    .longitude(BigDecimal.valueOf(PICKUP_LON))
                    .speedKmh(BigDecimal.ZERO)
                    .ignitionOn(true)
                    .build();

            // When
            trackingService.ingestTelemetry(locationDto);

            // Then: Should not query Redis for destination
            verify(valueOperations, never()).get(contains(":destination"));
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should handle missing destination gracefully")
        void shouldHandleMissingDestinationGracefully() {
            // Given: No destination registered in Redis
            Long tripId = 100L;
            String destinationKey = "order:" + tripId + ":destination";
            when(valueOperations.get(destinationKey)).thenReturn(null);

            LocationDto locationDto = LocationDto.builder()
                    .time(OffsetDateTime.now())
                    .vehicleId(101L)
                    .tripId(tripId)
                    .latitude(BigDecimal.valueOf(PICKUP_LAT))
                    .longitude(BigDecimal.valueOf(PICKUP_LON))
                    .speedKmh(BigDecimal.valueOf(20))
                    .ignitionOn(true)
                    .build();

            // When & Then: Should not throw, should not emit event
            trackingService.ingestTelemetry(locationDto);
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Batch Ingestion Tests")
    class BatchIngestionTests {

        @Test
        @DisplayName("should process batch of telemetry records")
        void shouldProcessBatchOfTelemetry() {
            // Given: Multiple telemetry records
            List<LocationDto> batch = List.of(
                    createLocationDto(101L, null),
                    createLocationDto(102L, null),
                    createLocationDto(103L, null)
            );

            // When
            trackingService.ingestTelemetryBatch(batch);

            // Then: All records should be saved
            verify(locationRepository).saveAll(argThat(list -> 
                    ((List<?>) list).size() == 3
            ));
        }
    }

    @Nested
    @DisplayName("Register Destination Tests")
    class RegisterDestinationTests {

        @Test
        @DisplayName("should register order destination in Redis")
        void shouldRegisterDestination() {
            // When
            trackingService.registerOrderDestination(
                    100L,
                    BigDecimal.valueOf(PICKUP_LAT),
                    BigDecimal.valueOf(PICKUP_LON)
            );

            // Then
            verify(valueOperations).set(
                    eq("order:100:destination"),
                    contains(String.valueOf(PICKUP_LAT)),
                    eq(Duration.ofHours(24))
            );
        }
    }

    private LocationDto createLocationDto(Long vehicleId, Long tripId) {
        return LocationDto.builder()
                .time(OffsetDateTime.now())
                .vehicleId(vehicleId)
                .tripId(tripId)
                .latitude(BigDecimal.valueOf(PICKUP_LAT))
                .longitude(BigDecimal.valueOf(PICKUP_LON))
                .speedKmh(BigDecimal.valueOf(20))
                .ignitionOn(true)
                .build();
    }

    // Test helper record matching the one in TrackingService
    private record DestinationPoint(double latitude, double longitude) {}
}
