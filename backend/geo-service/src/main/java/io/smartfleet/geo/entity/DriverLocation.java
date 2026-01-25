package io.smartfleet.geo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;

/**
 * Entity for driver/vehicle live locations.
 * Uses PostGIS GEOMETRY type for spatial queries.
 */
@Entity
@Table(name = "driver_locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    /**
     * PostGIS Point geometry (SRID 4326 - WGS84)
     */
    @Column(name = "location", columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point location;

    @Column(name = "speed_kmh")
    private Double speedKmh;

    @Column(name = "heading_degrees")
    private Double headingDegrees;

    @Column(name = "status")
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(name = "last_updated", nullable = false)
    @Builder.Default
    private OffsetDateTime lastUpdated = OffsetDateTime.now();

    @Column(name = "accuracy_meters")
    private Double accuracyMeters;

    /**
     * Current trip/order ID if driver is on a delivery
     */
    @Column(name = "current_order_id")
    private Long currentOrderId;

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = OffsetDateTime.now();
    }
}
