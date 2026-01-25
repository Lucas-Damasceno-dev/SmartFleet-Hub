package io.smartfleet.tracking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entity for GPS location telemetry (TimescaleDB Hypertable).
 */
@Entity
@Table(name = "locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Column(name = "time", nullable = false)
    private OffsetDateTime time;

    @Id
    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "speed_kmh", precision = 5, scale = 2)
    private BigDecimal speedKmh;

    @Column(name = "heading_degrees", precision = 5, scale = 2)
    private BigDecimal headingDegrees;

    @Column(name = "battery_level", precision = 5, scale = 2)
    private BigDecimal batteryLevel;

    @Column(name = "ignition_on")
    private Boolean ignitionOn;

    @Column(name = "accuracy_meters", precision = 6, scale = 2)
    private BigDecimal accuracyMeters;
}
