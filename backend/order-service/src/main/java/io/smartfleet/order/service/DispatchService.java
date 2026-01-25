package io.smartfleet.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smartfleet.common.event.OrderAssignedPayload;
import io.smartfleet.common.event.OrderEvents;
import io.smartfleet.common.exception.BusinessRuleException;
import io.smartfleet.common.exception.ResourceNotFoundException;
import io.smartfleet.order.entity.DispatchStatus;
import io.smartfleet.order.entity.Order;
import io.smartfleet.order.entity.OrderStatus;
import io.smartfleet.order.entity.Outbox;
import io.smartfleet.order.repository.OrderRepository;
import io.smartfleet.order.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service implementing the Greedy-Proximity with Timeout Waterfall dispatch algorithm.
 * 
 * Algorithm:
 * 1. CREATED -> SEARCHING: Start searching for drivers within 5km radius
 * 2. SEARCHING: Query nearest available drivers, sorted by distance
 * 3. Attempt to assign the closest driver
 * 4. If no driver available/accepts within timeout, expand radius or fail
 * 5. SEARCHING -> ASSIGNED: Driver confirmed
 * 
 * State Machine:
 * CREATED -> SEARCHING -> ASSIGNED (success)
 *                      -> NO_DRIVER_FOUND (timeout/failure)
 *                      -> CANCELLED (user cancelled)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    // Dispatch configuration
    @Value("${dispatch.initial-radius-km:5.0}")
    private double initialRadiusKm;

    @Value("${dispatch.max-radius-km:15.0}")
    private double maxRadiusKm;

    @Value("${dispatch.radius-increment-km:5.0}")
    private double radiusIncrementKm;

    @Value("${dispatch.timeout-per-tier-seconds:30}")
    private int timeoutPerTierSeconds;

    @Value("${dispatch.max-attempts:3}")
    private int maxAttempts;

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final GeoServiceClient geoServiceClient;
    private final ObjectMapper objectMapper;

    /**
     * Start dispatch process for an order.
     * Transitions order from CREATED/PENDING -> SEARCHING
     *
     * @param orderId Order to dispatch
     * @return CompletableFuture with dispatch result
     */
    @Async
    public CompletableFuture<DispatchResult> startDispatch(Long orderId) {
        log.info("Starting dispatch for order {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Validate current state
        if (!canStartDispatch(order)) {
            throw new BusinessRuleException(
                    "Cannot start dispatch for order in status: " + order.getStatus(),
                    "INVALID_ORDER_STATUS");
        }

        // Transition to SEARCHING
        transitionToSearching(order);

        // Execute dispatch algorithm
        DispatchResult result = executeGreedyProximityDispatch(order);

        return CompletableFuture.completedFuture(result);
    }

    /**
     * Execute the Greedy-Proximity with Timeout Waterfall algorithm.
     */
    private DispatchResult executeGreedyProximityDispatch(Order order) {
        double currentRadius = initialRadiusKm;
        int attempt = 0;

        while (attempt < maxAttempts && currentRadius <= maxRadiusKm) {
            attempt++;
            log.info("Dispatch attempt {} for order {} - radius: {}km", attempt, order.getOrderId(), currentRadius);

            // Find drivers within current radius
            List<AvailableDriver> nearbyDrivers = geoServiceClient.findDriversWithinRadius(
                    order.getPickupLatitude(),
                    order.getPickupLongitude(),
                    currentRadius
            );

            if (!nearbyDrivers.isEmpty()) {
                // Greedy: pick the closest available driver
                AvailableDriver closestDriver = nearbyDrivers.get(0);
                log.info("Found closest driver: {} at {}km for order {}",
                        closestDriver.getDriverName(), closestDriver.getDistanceKm(), order.getOrderId());

                // Attempt to assign driver
                boolean assigned = tryAssignDriver(order, closestDriver);
                if (assigned) {
                    return DispatchResult.success(order.getOrderId(), closestDriver);
                }
            }

            // Waterfall: expand radius for next tier
            currentRadius += radiusIncrementKm;
            log.debug("No available drivers in radius, expanding to {}km", currentRadius);

            // Simulate timeout wait (in production, this would be actual driver response wait)
            try {
                Thread.sleep(100); // Simplified for testing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // No driver found after all attempts
        log.warn("No driver found for order {} after {} attempts", order.getOrderId(), attempt);
        transitionToNoDriverFound(order);

        return DispatchResult.failure(order.getOrderId(), "No available drivers found");
    }

    /**
     * Attempt to assign a driver to an order.
     */
    @Transactional
    public boolean tryAssignDriver(Order order, AvailableDriver driver) {
        try {
            // Update order with driver assignment
            order.setAssignedDriverId(driver.getDriverId());
            order.setAssignedVehicleId(driver.getVehicleId());
            order.setStatus(OrderStatus.ASSIGNED.name());

            Order updatedOrder = orderRepository.save(order);

            // Publish ORDER_ASSIGNED event
            publishOrderAssignedEvent(updatedOrder, driver);

            log.info("Order {} assigned to driver {} (vehicle {})",
                    order.getOrderId(), driver.getDriverId(), driver.getVehicleId());

            return true;
        } catch (Exception e) {
            log.error("Failed to assign driver {} to order {}", driver.getDriverId(), order.getOrderId(), e);
            return false;
        }
    }

    private boolean canStartDispatch(Order order) {
        String status = order.getStatus();
        return OrderStatus.PENDING.name().equals(status) ||
               DispatchStatus.CREATED.name().equals(status);
    }

    @Transactional
    private void transitionToSearching(Order order) {
        log.info("Order {} transitioning to SEARCHING", order.getOrderId());
        order.setStatus(OrderStatus.DRIVER_EN_ROUTE.name()); // Using existing status
        orderRepository.save(order);
    }

    @Transactional
    private void transitionToNoDriverFound(Order order) {
        log.info("Order {} transitioning to NO_DRIVER_FOUND (back to PENDING)", order.getOrderId());
        order.setStatus(OrderStatus.PENDING.name()); // Back to PENDING for retry
        orderRepository.save(order);
    }

    private void publishOrderAssignedEvent(Order order, AvailableDriver driver) {
        try {
            OrderAssignedPayload payload = OrderAssignedPayload.builder()
                    .orderId(order.getOrderId())
                    .driverId(driver.getDriverId())
                    .driverName(driver.getDriverName())
                    .vehicleId(driver.getVehicleId())
                    .vehiclePlate(driver.getVehiclePlate())
                    .assignedAt(OffsetDateTime.now())
                    .build();

            String payloadJson = objectMapper.writeValueAsString(payload);

            Outbox outbox = Outbox.builder()
                    .id(UUID.randomUUID())
                    .aggregateType(OrderEvents.AGGREGATE_TYPE)
                    .aggregateId(order.getOrderId().toString())
                    .type(OrderEvents.ORDER_ASSIGNED)
                    .payload(payloadJson)
                    .build();

            outboxRepository.save(outbox);
            log.debug("Published ORDER_ASSIGNED event for order {}", order.getOrderId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ORDER_ASSIGNED payload for order {}", order.getOrderId(), e);
        }
    }

    /**
     * Dispatch result DTO.
     */
    public record DispatchResult(
            Long orderId,
            boolean success,
            AvailableDriver assignedDriver,
            String errorMessage
    ) {
        public static DispatchResult success(Long orderId, AvailableDriver driver) {
            return new DispatchResult(orderId, true, driver, null);
        }

        public static DispatchResult failure(Long orderId, String errorMessage) {
            return new DispatchResult(orderId, false, null, errorMessage);
        }
    }
}
