package io.smartfleet.order.entity;

/**
 * Enum for order status values.
 */
public enum OrderStatus {
    PENDING,
    ASSIGNED,
    DRIVER_EN_ROUTE,
    PICKED_UP,
    IN_TRANSIT,
    NEARBY,
    DELIVERED,
    CANCELLED
}
