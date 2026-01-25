package io.smartfleet.tracking.consumer;

import io.smartfleet.common.dto.LocationDto;
import io.smartfleet.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for GPS telemetry events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryConsumer {

    private final TrackingService trackingService;

    @KafkaListener(
            topics = "${kafka.topics.telemetry:telemetry-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTelemetry(LocationDto locationDto) {
        log.info("Received telemetry for vehicle {}: lat={}, lon={}, speed={}",
                locationDto.getVehicleId(),
                locationDto.getLatitude(),
                locationDto.getLongitude(),
                locationDto.getSpeedKmh());

        try {
            trackingService.ingestTelemetry(locationDto);
        } catch (Exception e) {
            log.error("Failed to process telemetry for vehicle {}", locationDto.getVehicleId(), e);
            throw e;
        }
    }
}
