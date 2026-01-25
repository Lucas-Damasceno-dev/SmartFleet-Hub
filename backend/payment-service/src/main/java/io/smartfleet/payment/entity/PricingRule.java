package io.smartfleet.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entity for pricing rules per vehicle type.
 */
@Entity
@Table(name = "pricing_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_type_id")
    private Integer vehicleTypeId;

    @Column(name = "base_fee", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal baseFee = BigDecimal.valueOf(5.00);

    @Column(name = "rate_km", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal rateKm = BigDecimal.valueOf(1.50);

    @Column(name = "rate_min", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal rateMin = BigDecimal.valueOf(0.50);

    @Column(name = "minimum_fare", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal minimumFare = BigDecimal.valueOf(10.00);

    @Column(name = "multiplier", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal multiplier = BigDecimal.valueOf(1.00);

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
