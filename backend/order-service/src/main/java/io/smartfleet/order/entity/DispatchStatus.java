package io.smartfleet.order.entity;

/**
 * Enum for dispatch/search status.
 */
public enum DispatchStatus {
    CREATED,
    SEARCHING,
    ASSIGNED,
    NO_DRIVER_FOUND,
    CANCELLED
}
