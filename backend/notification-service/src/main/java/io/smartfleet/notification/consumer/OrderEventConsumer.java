package io.smartfleet.notification.consumer;

import io.smartfleet.common.event.OrderAssignedPayload;
import io.smartfleet.common.event.OrderEvents;
import io.smartfleet.notification.service.FirebaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for order events to trigger notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final FirebaseService firebaseService;

    @KafkaListener(
            topics = "outbox.event.ORDER",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeOrderEvent(
            @Payload String payload,
            @Header(value = "eventType", required = false) String eventType) {
        
        log.info("Received order event: type={}", eventType);

        if (eventType == null) {
            log.warn("Event type is null, skipping");
            return;
        }

        try {
            switch (eventType) {
                case OrderEvents.ORDER_ASSIGNED -> handleOrderAssigned(payload);
                case OrderEvents.DRIVER_NEARBY -> handleDriverNearby(payload);
                case OrderEvents.ORDER_DELIVERED -> handleOrderDelivered(payload);
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process order event: {}", eventType, e);
        }
    }

    private void handleOrderAssigned(String payload) {
        log.info("Processing ORDER_ASSIGNED event");
        // In production, parse payload and get customer device token from user service
        // For now, log the event
        log.info("Would send ORDER_ASSIGNED notification for payload: {}", payload);
        
        // Example:
        // OrderAssignedPayload event = objectMapper.readValue(payload, OrderAssignedPayload.class);
        // String customerDeviceToken = userService.getDeviceToken(event.getCustomerId());
        // firebaseService.sendOrderAssignedNotification(customerDeviceToken, event.getDriverName(), event.getOrderId());
    }

    private void handleDriverNearby(String payload) {
        log.info("Processing DRIVER_NEARBY event");
        log.info("Would send NEARBY notification for payload: {}", payload);
    }

    private void handleOrderDelivered(String payload) {
        log.info("Processing ORDER_DELIVERED event");
        log.info("Would send DELIVERED notification for payload: {}", payload);
    }
}
