package io.smartfleet.notification.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smartfleet.common.event.OrderEvents;
import io.smartfleet.notification.service.DeviceTokenService;
import io.smartfleet.notification.service.FirebaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Kafka consumer for order events to trigger notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final FirebaseService firebaseService;
    private final DeviceTokenService deviceTokenService;
    private final ObjectMapper objectMapper;

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
        
        try {
            JsonNode node = objectMapper.readTree(payload);
            Long orderId = node.get("orderId").asLong();
            String driverIdStr = node.has("driverId") ? node.get("driverId").asText() : null;
            String driverName = node.has("driverName") ? node.get("driverName").asText() : "Your driver";
            String customerIdStr = node.has("customerId") ? node.get("customerId").asText() : null;

            // Send notification to driver about new assignment
            if (driverIdStr != null) {
                UUID driverId = UUID.fromString(driverIdStr);
                List<String> driverTokens = deviceTokenService.getActiveTokensForDriver(driverId);
                
                for (String token : driverTokens) {
                    firebaseService.sendPushNotification(
                            io.smartfleet.notification.dto.PushNotificationRequest.builder()
                                    .deviceToken(token)
                                    .title("New Order Assigned")
                                    .body("You have a new delivery order #" + orderId)
                                    .data(java.util.Map.of(
                                            "type", "ORDER_ASSIGNED",
                                            "orderId", orderId.toString(),
                                            "action", "VIEW_ORDER"
                                    ))
                                    .build()
                    );
                }
                log.info("Sent ORDER_ASSIGNED notification to driver: {}", driverId);
            }

            // Send notification to customer about driver assignment
            if (customerIdStr != null) {
                UUID customerId = UUID.fromString(customerIdStr);
                List<String> customerTokens = deviceTokenService.getActiveTokensForUser(customerId);
                
                for (String token : customerTokens) {
                    firebaseService.sendOrderAssignedNotification(token, driverName, orderId);
                }
                log.info("Sent ORDER_ASSIGNED notification to customer: {}", customerId);
            }
        } catch (Exception e) {
            log.error("Failed to parse ORDER_ASSIGNED payload", e);
        }
    }

    private void handleDriverNearby(String payload) {
        log.info("Processing DRIVER_NEARBY event");
        
        try {
            JsonNode node = objectMapper.readTree(payload);
            Long orderId = node.get("orderId").asLong();
            String customerIdStr = node.has("customerId") ? node.get("customerId").asText() : null;

            if (customerIdStr != null) {
                UUID customerId = UUID.fromString(customerIdStr);
                List<String> customerTokens = deviceTokenService.getActiveTokensForUser(customerId);
                
                for (String token : customerTokens) {
                    firebaseService.sendNearbyNotification(token, orderId);
                }
                log.info("Sent DRIVER_NEARBY notification to customer: {}", customerId);
            }
        } catch (Exception e) {
            log.error("Failed to parse DRIVER_NEARBY payload", e);
        }
    }

    private void handleOrderDelivered(String payload) {
        log.info("Processing ORDER_DELIVERED event");
        
        try {
            JsonNode node = objectMapper.readTree(payload);
            Long orderId = node.get("orderId").asLong();
            String customerIdStr = node.has("customerId") ? node.get("customerId").asText() : null;

            if (customerIdStr != null) {
                UUID customerId = UUID.fromString(customerIdStr);
                List<String> customerTokens = deviceTokenService.getActiveTokensForUser(customerId);
                
                for (String token : customerTokens) {
                    firebaseService.sendDeliveredNotification(token, orderId);
                }
                log.info("Sent ORDER_DELIVERED notification to customer: {}", customerId);
            }
        } catch (Exception e) {
            log.error("Failed to parse ORDER_DELIVERED payload", e);
        }
    }
}
