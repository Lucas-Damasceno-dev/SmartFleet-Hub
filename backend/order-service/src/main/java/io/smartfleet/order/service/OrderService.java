package io.smartfleet.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smartfleet.common.dto.OrderDto;
import io.smartfleet.common.event.OrderCreatedPayload;
import io.smartfleet.common.event.OrderEvents;
import io.smartfleet.common.exception.ResourceNotFoundException;
import io.smartfleet.order.entity.Order;
import io.smartfleet.order.entity.OrderStatus;
import io.smartfleet.order.entity.Outbox;
import io.smartfleet.order.mapper.OrderMapper;
import io.smartfleet.order.repository.OrderRepository;
import io.smartfleet.order.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for order management with Transactional Outbox pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderDto createOrder(OrderDto orderDto) {
        log.info("Creating new order for customer: {}", orderDto.getCustomerName());

        Order order = orderMapper.toEntity(orderDto);
        order.setStatus(OrderStatus.PENDING.name());
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getOrderId());

        // Create outbox event within the same transaction
        publishOutboxEvent(savedOrder, OrderEvents.ORDER_CREATED);

        return orderMapper.toDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return orderMapper.toDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status).stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Transactional
    public OrderDto assignDriver(Long orderId, Long driverId, Long vehicleId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        order.setAssignedDriverId(driverId);
        order.setAssignedVehicleId(vehicleId);
        order.setStatus(OrderStatus.ASSIGNED.name());

        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} assigned to driver {}", orderId, driverId);

        publishOutboxEvent(updatedOrder, OrderEvents.ORDER_ASSIGNED);

        return orderMapper.toDto(updatedOrder);
    }

    @Transactional
    public OrderDto updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        String oldStatus = order.getStatus();
        order.setStatus(newStatus.name());

        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} status changed from {} to {}", orderId, oldStatus, newStatus);

        publishOutboxEvent(updatedOrder, OrderEvents.STATUS_CHANGED);

        return orderMapper.toDto(updatedOrder);
    }

    private void publishOutboxEvent(Order order, String eventType) {
        try {
            OrderCreatedPayload payload = OrderCreatedPayload.builder()
                    .orderId(order.getOrderId())
                    .customerName(order.getCustomerName())
                    .customerPhone(order.getCustomerPhone())
                    .pickupLatitude(order.getPickupLatitude())
                    .pickupLongitude(order.getPickupLongitude())
                    .pickupAddress(order.getPickupAddress())
                    .deliveryLatitude(order.getDeliveryLatitude())
                    .deliveryLongitude(order.getDeliveryLongitude())
                    .deliveryAddress(order.getDeliveryAddress())
                    .scheduledPickupTime(order.getScheduledPickupTime())
                    .createdAt(order.getCreatedAt())
                    .build();

            String payloadJson = objectMapper.writeValueAsString(payload);

            Outbox outbox = Outbox.builder()
                    .id(UUID.randomUUID())
                    .aggregateType(OrderEvents.AGGREGATE_TYPE)
                    .aggregateId(order.getOrderId().toString())
                    .type(eventType)
                    .payload(payloadJson)
                    .build();

            outboxRepository.save(outbox);
            log.debug("Outbox event {} published for order {}", eventType, order.getOrderId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox payload for order {}", order.getOrderId(), e);
            throw new RuntimeException("Failed to publish outbox event", e);
        }
    }
}
