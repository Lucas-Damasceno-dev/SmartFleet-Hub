package io.smartfleet.tracking.consumer;

import io.smartfleet.common.dto.LocationDto;
import io.smartfleet.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka consumer for GPS telemetry events.
 * Optimized for high-speed ingestion with batch processing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryConsumer {

    private final TrackingService trackingService;

    /**
     * Single message consumer for low-latency processing.
     */
    @KafkaListener(
            topics = "${kafka.topics.telemetry:telemetry-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTelemetry(LocationDto locationDto) {
        log.debug("Received telemetry for vehicle {}: lat={}, lon={}, speed={}",
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

    /**
     * Batch consumer for high-throughput scenarios.
     * Processes multiple messages at once for better DB efficiency.
     */
    @KafkaListener(
            topics = "${kafka.topics.telemetry-batch:telemetry-batch-topic}",
            groupId = "${spring.kafka.consumer.group-id}-batch",
            containerFactory = "batchKafkaListenerContainerFactory",
            autoStartup = "${kafka.batch-consumer.enabled:false}"
    )
    public void consumeTelemetryBatch(List<LocationDto> locationDtos, Acknowledgment ack) {
        log.info("Received batch of {} telemetry messages", locationDtos.size());

        try {
            trackingService.ingestTelemetryBatch(locationDtos);
            ack.acknowledge();
            log.debug("Batch of {} messages processed and acknowledged", locationDtos.size());
        } catch (Exception e) {
            log.error("Failed to process telemetry batch of {} messages", locationDtos.size(), e);
            throw e;
        }
    }
}
