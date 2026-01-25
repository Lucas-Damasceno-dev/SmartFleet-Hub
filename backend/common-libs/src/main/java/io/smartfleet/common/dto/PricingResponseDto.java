package io.smartfleet.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for pricing calculation response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingResponseDto {

    private Long orderId;
    private BigDecimal baseFee;
    private BigDecimal distanceCharge;
    private BigDecimal durationCharge;
    private BigDecimal multiplier;
    private BigDecimal subtotal;
    private BigDecimal minimumFare;
    private BigDecimal finalPrice;
}
