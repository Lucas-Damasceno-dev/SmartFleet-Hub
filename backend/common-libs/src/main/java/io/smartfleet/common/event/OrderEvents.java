package io.smartfleet.common.event;

/**
 * Constants for order-related events.
 */
public final class OrderEvents {

    private OrderEvents() {}

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_ASSIGNED = "ORDER_ASSIGNED";
    public static final String ORDER_PICKED_UP = "ORDER_PICKED_UP";
    public static final String ORDER_DELIVERED = "ORDER_DELIVERED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String DRIVER_NEARBY = "DRIVER_NEARBY";
    public static final String STATUS_CHANGED = "STATUS_CHANGED";

    public static final String AGGREGATE_TYPE = "ORDER";
}
