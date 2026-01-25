package io.smartfleet.geo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;

/**
 * Entity for order delivery locations (pickup/delivery points).
 */
@Entity
@Table(name = "order_locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLocation {

    @Id
    private Long orderId;

    @Column(name = "pickup_location", columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point pickupLocation;

    @Column(name = "delivery_location", columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point deliveryLocation;

    @Column(name = "pickup_address")
    private String pickupAddress;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "assigned_driver_id")
    private Long assignedDriverId;

    @Column(name = "status")
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
