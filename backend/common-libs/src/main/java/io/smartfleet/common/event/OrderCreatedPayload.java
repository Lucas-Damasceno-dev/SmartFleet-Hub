package io.smartfleet.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Payload for ORDER_CREATED event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedPayload {

    private Long orderId;
    private String customerName;
    private String customerPhone;
    private BigDecimal pickupLatitude;
    private BigDecimal pickupLongitude;
    private String pickupAddress;
    private BigDecimal deliveryLatitude;
    private BigDecimal deliveryLongitude;
    private String deliveryAddress;
    private OffsetDateTime scheduledPickupTime;
    private OffsetDateTime createdAt;
}
