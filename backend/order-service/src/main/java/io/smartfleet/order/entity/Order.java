package io.smartfleet.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entity representing a delivery order.
 */
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "pickup_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal pickupLatitude;

    @Column(name = "pickup_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal pickupLongitude;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "delivery_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal deliveryLongitude;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "assigned_driver_id")
    private Long assignedDriverId;

    @Column(name = "assigned_vehicle_id")
    private Long assignedVehicleId;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = OrderStatus.PENDING.name();

    @Column(name = "estimated_price", precision = 10, scale = 2)
    private BigDecimal estimatedPrice;

    @Column(name = "final_price", precision = 10, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "scheduled_pickup_time")
    private OffsetDateTime scheduledPickupTime;

    @Column(name = "actual_pickup_time")
    private OffsetDateTime actualPickupTime;

    @Column(name = "actual_delivery_time")
    private OffsetDateTime actualDeliveryTime;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
