package io.smartfleet.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for Order/Delivery request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    private Long orderId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String customerPhone;
    private String customerEmail;

    @NotNull(message = "Pickup latitude is required")
    private BigDecimal pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    private BigDecimal pickupLongitude;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotNull(message = "Delivery latitude is required")
    private BigDecimal deliveryLatitude;

    @NotNull(message = "Delivery longitude is required")
    private BigDecimal deliveryLongitude;

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;

    private Long assignedDriverId;
    private Long assignedVehicleId;
    private String status;
    private BigDecimal estimatedPrice;
    private BigDecimal finalPrice;
    private OffsetDateTime scheduledPickupTime;
    private OffsetDateTime actualPickupTime;
    private OffsetDateTime actualDeliveryTime;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
