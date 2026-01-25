package io.smartfleet.payment.service;

import io.smartfleet.common.dto.PricingRequestDto;
import io.smartfleet.common.dto.PricingResponseDto;
import io.smartfleet.payment.entity.PricingRule;
import io.smartfleet.payment.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for pricing calculations.
 * Implements deterministic pricing algorithm:
 * Price = (BaseFee + (Distance_km × Rate_km) + (Duration_min × Rate_min)) × Multiplier
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private static final int DECIMAL_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    // Default pricing constants (used if no rule found in DB)
    private static final BigDecimal DEFAULT_BASE_FEE = new BigDecimal("5.00");
    private static final BigDecimal DEFAULT_RATE_KM = new BigDecimal("1.50");
    private static final BigDecimal DEFAULT_RATE_MIN = new BigDecimal("0.50");
    private static final BigDecimal DEFAULT_MINIMUM_FARE = new BigDecimal("10.00");
    private static final BigDecimal DEFAULT_MULTIPLIER = new BigDecimal("1.00");

    private final PricingRuleRepository pricingRuleRepository;

    /**
     * Calculate price for an order.
     *
     * @param request Pricing request with distance and duration
     * @return Pricing response with detailed breakdown
     */
    public PricingResponseDto calculatePrice(PricingRequestDto request) {
        log.debug("Calculating price for order {} - distance: {} km, duration: {} min",
                request.getOrderId(), request.getDistanceKm(), request.getDurationMinutes());

        // Get pricing rule for vehicle type
        PricingRule rule = getPricingRule(request.getVehicleTypeId());

        BigDecimal baseFee = rule.getBaseFee();
        BigDecimal rateKm = rule.getRateKm();
        BigDecimal rateMin = rule.getRateMin();
        BigDecimal minimumFare = rule.getMinimumFare();
        BigDecimal multiplier = rule.getMultiplier();

        // Calculate components
        BigDecimal distanceCharge = request.getDistanceKm()
                .multiply(rateKm)
                .setScale(DECIMAL_SCALE, ROUNDING_MODE);

        BigDecimal durationCharge = request.getDurationMinutes()
                .multiply(rateMin)
                .setScale(DECIMAL_SCALE, ROUNDING_MODE);

        // Calculate subtotal: BaseFee + DistanceCharge + DurationCharge
        BigDecimal subtotal = baseFee
                .add(distanceCharge)
                .add(durationCharge)
                .setScale(DECIMAL_SCALE, ROUNDING_MODE);

        // Apply multiplier (surge pricing - currently 1.0)
        BigDecimal priceBeforeMinimum = subtotal
                .multiply(multiplier)
                .setScale(DECIMAL_SCALE, ROUNDING_MODE);

        // Apply minimum fare
        BigDecimal finalPrice = priceBeforeMinimum.max(minimumFare);

        log.info("Price calculated for order {}: base={}, distance={}, duration={}, subtotal={}, final={}",
                request.getOrderId(), baseFee, distanceCharge, durationCharge, subtotal, finalPrice);

        return PricingResponseDto.builder()
                .orderId(request.getOrderId())
                .baseFee(baseFee)
                .distanceCharge(distanceCharge)
                .durationCharge(durationCharge)
                .multiplier(multiplier)
                .subtotal(subtotal)
                .minimumFare(minimumFare)
                .finalPrice(finalPrice)
                .build();
    }

    /**
     * Get pricing rule for vehicle type, or default if not found.
     */
    private PricingRule getPricingRule(Integer vehicleTypeId) {
        if (vehicleTypeId != null) {
            return pricingRuleRepository.findByVehicleTypeIdAndActiveTrue(vehicleTypeId)
                    .orElseGet(this::getDefaultPricingRule);
        }
        return pricingRuleRepository.findFirstByActiveTrue()
                .orElseGet(this::getDefaultPricingRule);
    }

    /**
     * Create default pricing rule if none found in database.
     */
    private PricingRule getDefaultPricingRule() {
        log.warn("No pricing rule found, using default values");
        return PricingRule.builder()
                .baseFee(DEFAULT_BASE_FEE)
                .rateKm(DEFAULT_RATE_KM)
                .rateMin(DEFAULT_RATE_MIN)
                .minimumFare(DEFAULT_MINIMUM_FARE)
                .multiplier(DEFAULT_MULTIPLIER)
                .build();
    }
}
