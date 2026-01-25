package io.smartfleet.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Payload for ORDER_ASSIGNED event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAssignedPayload {

    private Long orderId;
    private Long driverId;
    private String driverName;
    private Long vehicleId;
    private String vehiclePlate;
    private OffsetDateTime assignedAt;
}
