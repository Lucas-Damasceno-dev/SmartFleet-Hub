package io.smartfleet.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for pricing calculation request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRequestDto {

    private Long orderId;
    private Integer vehicleTypeId;
    private BigDecimal distanceKm;
    private BigDecimal durationMinutes;
}
