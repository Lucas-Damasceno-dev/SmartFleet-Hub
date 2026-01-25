package io.smartfleet.payment.service;

import io.smartfleet.common.dto.PricingRequestDto;
import io.smartfleet.common.dto.PricingResponseDto;
import io.smartfleet.payment.entity.PricingRule;
import io.smartfleet.payment.repository.PricingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PricingService.
 * Tests the pricing formula: (BaseFee + (Distance * RateKm) + (Duration * RateMin)) * Multiplier
 */
@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @InjectMocks
    private PricingService pricingService;

    // Default pricing constants matching the implementation
    private static final BigDecimal DEFAULT_BASE_FEE = new BigDecimal("5.00");
    private static final BigDecimal DEFAULT_RATE_KM = new BigDecimal("1.50");
    private static final BigDecimal DEFAULT_RATE_MIN = new BigDecimal("0.50");
    private static final BigDecimal DEFAULT_MINIMUM_FARE = new BigDecimal("10.00");
    private static final BigDecimal DEFAULT_MULTIPLIER = new BigDecimal("1.00");

    @Nested
    @DisplayName("calculatePrice with default pricing rule")
    class DefaultPricingTests {

        @BeforeEach
        void setUp() {
            // Return empty to trigger default rule
            when(pricingRuleRepository.findFirstByActiveTrue()).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("should calculate price correctly with standard values")
        void shouldCalculatePriceWithStandardValues() {
            // Given: 10km distance, 15 minutes duration
            PricingRequestDto request = PricingRequestDto.builder()
                    .orderId(1L)
                    .distanceKm(new BigDecimal("10.00"))
                    .durationMinutes(new BigDecimal("15.00"))
                    .build();

            // When
            PricingResponseDto result = pricingService.calculatePrice(request);

            // Then
            // Expected: (5.00 + (10.00 * 1.50) + (15.00 * 0.50)) * 1.00 = 5.00 + 15.00 + 7.50 = 27.50
            assertThat(result.getOrderId()).isEqualTo(1L);
            assertThat(result.getBaseFee()).isEqualByComparingTo(DEFAULT_BASE_FEE);
            assertThat(result.getDistanceCharge()).isEqualByComparingTo(new BigDecimal("15.00"));
            assertThat(result.getDurationCharge()).isEqualByComparingTo(new BigDecimal("7.50"));
            assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("27.50"));
            assertThat(result.getFinalPrice()).isEqualByComparingTo(new BigDecimal("27.50"));
        }

        @Test
        @DisplayName("should apply minimum fare when calculated price is below minimum")
        void shouldApplyMinimumFare() {
            // Given: 1km distance, 2 minutes duration
            PricingRequestDto request = PricingRequestDto.builder()
                    .orderId(2L)
                    .distanceKm(new BigDecimal("1.00"))
                    .durationMinutes(new BigDecimal("2.00"))
                    .build();

            // When
            PricingResponseDto result = pricingService.calculatePrice(request);

            // Then
            // Calculated: (5.00 + (1.00 * 1.50) + (2.00 * 0.50)) * 1.00 = 5.00 + 1.50 + 1.00 = 7.50
            // Minimum fare: 10.00 (should be applied)
            assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("7.50"));
            assertThat(result.getMinimumFare()).isEqualByComparingTo(DEFAULT_MINIMUM_FARE);
            assertThat(result.getFinalPrice()).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("should handle zero distance and duration")
        void shouldHandleZeroValues() {
            // Given
            PricingRequestDto request = PricingRequestDto.builder()
                    .orderId(3L)
                    .distanceKm(BigDecimal.ZERO)
                    .durationMinutes(BigDecimal.ZERO)
                    .build();

            // When
            PricingResponseDto result = pricingService.calculatePrice(request);

            // Then
            // Calculated: 5.00 + 0 + 0 = 5.00 -> minimum fare 10.00 applies
            assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(result.getFinalPrice()).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @ParameterizedTest
        @CsvSource({
            "5.00,  10.00, 17.50",   // 5km, 10min -> 5 + 7.5 + 5 = 17.50
            "20.00, 30.00, 50.00",   // 20km, 30min -> 5 + 30 + 15 = 50.00
            "0.50,  1.00,  10.00",   // 0.5km, 1min -> below minimum -> 10.00
            "100.00, 60.00, 185.00"  // 100km, 60min -> 5 + 150 + 30 = 185.00
        })
        @DisplayName("should calculate correct prices for various inputs")
        void shouldCalculatePriceForVariousInputs(String distanceKm, String durationMin, String expectedPrice) {
            // Given
            PricingRequestDto request = PricingRequestDto.builder()
                    .orderId(100L)
                    .distanceKm(new BigDecimal(distanceKm))
                    .durationMinutes(new BigDecimal(durationMin))
                    .build();

            // When
            PricingResponseDto result = pricingService.calculatePrice(request);

            // Then
            assertThat(result.getFinalPrice()).isEqualByComparingTo(new BigDecimal(expectedPrice));
        }
    }

    @Nested
    @DisplayName("calculatePrice with custom pricing rule")
    class CustomPricingTests {

        @Test
        @DisplayName("should use vehicle-specific pricing rule when available")
        void shouldUseVehicleSpecificRule() {
            // Given: Premium vehicle pricing
            PricingRule premiumRule = PricingRule.builder()
                    .vehicleTypeId(2)
                    .baseFee(new BigDecimal("7.00"))
                    .rateKm(new BigDecimal("2.00"))
                    .rateMin(new BigDecimal("0.70"))
                    .minimumFare(new BigDecimal("15.00"))
                    .multiplier(new BigDecimal("1.00"))
                    .build();

            when(pricingRuleRepository.findByVehicleTypeIdAndActiveTrue(2))
                    .thenReturn(Optional.of(premiumRule));

            PricingRequestDto request = PricingRequestDto.builder()
                    .orderId(4L)
                    .vehicleTypeId(2)
                    .distanceKm(new BigDecimal("10.00"))
                    .durationMinutes(new BigDecimal("15.00"))
                    .build();

            // When
            PricingResponseDto result = pricingService.calculatePrice(request);

            // Then
            // Expected: (7.00 + (10.00 * 2.00) + (15.00 * 0.70)) * 1.00 = 7 + 20 + 10.5 = 37.50
            assertThat(result.getBaseFee()).isEqualByComparingTo(new BigDecimal("7.00"));
            assertThat(result.getDistanceCharge()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(result.getDurationCharge()).isEqualByComparingTo(new BigDecimal("10.50"));
            assertThat(result.getFinalPrice()).isEqualByComparingTo(new BigDecimal("37.50"));
        }

        @Test
        @DisplayName("should apply surge multiplier correctly")
        void shouldApplySurgeMultiplier() {
            // Given: Surge pricing (1.5x)
            PricingRule surgeRule = PricingRule.builder()
                    .baseFee(DEFAULT_BASE_FEE)
                    .rateKm(DEFAULT_RATE_KM)
                    .rateMin(DEFAULT_RATE_MIN)
                    .minimumFare(DEFAULT_MINIMUM_FARE)
                    .multiplier(new BigDecimal("1.50"))
                    .build();

            when(pricingRuleRepository.findByVehicleTypeIdAndActiveTrue(anyInt()))
                    .thenReturn(Optional.of(surgeRule));

            PricingRequestDto request = PricingRequestDto.builder()
                    .orderId(5L)
                    .vehicleTypeId(1)
                    .distanceKm(new BigDecimal("10.00"))
                    .durationMinutes(new BigDecimal("10.00"))
                    .build();

            // When
            PricingResponseDto result = pricingService.calculatePrice(request);

            // Then
            // Subtotal: 5 + 15 + 5 = 25.00
            // With 1.5x multiplier: 25.00 * 1.50 = 37.50
            assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(result.getMultiplier()).isEqualByComparingTo(new BigDecimal("1.50"));
            assertThat(result.getFinalPrice()).isEqualByComparingTo(new BigDecimal("37.50"));
        }

        @Test
        @DisplayName("should handle 2.0x surge with minimum fare")
        void shouldHandleHighSurgeWithMinimum() {
            // Given: 2.0x surge but very short trip
            PricingRule surgeRule = PricingRule.builder()
                    .baseFee(DEFAULT_BASE_FEE)
                    .rateKm(DEFAULT_RATE_KM)
                    .rateMin(DEFAULT_RATE_MIN)
                    .minimumFare(new BigDecimal("20.00")) // Higher minimum
                    .multiplier(new BigDecimal("2.00"))
                    .build();

            when(pricingRuleRepository.findFirstByActiveTrue())
                    .thenReturn(Optional.of(surgeRule));

            PricingRequestDto request = PricingRequestDto.builder()
                    .orderId(6L)
                    .distanceKm(new BigDecimal("1.00"))
                    .durationMinutes(new BigDecimal("2.00"))
                    .build();

            // When
            PricingResponseDto result = pricingService.calculatePrice(request);

            // Then
            // Subtotal: 5 + 1.5 + 1 = 7.50
            // With 2.0x: 7.50 * 2.0 = 15.00
            // Minimum fare 20.00 should apply
            assertThat(result.getFinalPrice()).isEqualByComparingTo(new BigDecimal("20.00"));
        }
    }

    @Nested
    @DisplayName("Edge cases and precision")
    class EdgeCaseTests {

        @BeforeEach
        void setUp() {
            when(pricingRuleRepository.findFirstByActiveTrue()).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("should handle decimal precision correctly")
        void shouldHandleDecimalPrecision() {
            // Given: Values that require rounding
            PricingRequestDto request = PricingRequestDto.builder()
                    .orderId(7L)
                    .distanceKm(new BigDecimal("3.333"))
                    .durationMinutes(new BigDecimal("7.777"))
                    .build();

            // When
            PricingResponseDto result = pricingService.calculatePrice(request);

            // Then
            // Distance: 3.333 * 1.50 = 4.9995 -> 5.00 (HALF_UP)
            // Duration: 7.777 * 0.50 = 3.8885 -> 3.89 (HALF_UP)
            // Subtotal: 5.00 + 5.00 + 3.89 = 13.89
            assertThat(result.getDistanceCharge().scale()).isEqualTo(2);
            assertThat(result.getDurationCharge().scale()).isEqualTo(2);
            assertThat(result.getFinalPrice().scale()).isLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("should handle large numbers")
        void shouldHandleLargeNumbers() {
            // Given: Very long trip
            PricingRequestDto request = PricingRequestDto.builder()
                    .orderId(8L)
                    .distanceKm(new BigDecimal("1000.00"))
                    .durationMinutes(new BigDecimal("600.00"))
                    .build();

            // When
            PricingResponseDto result = pricingService.calculatePrice(request);

            // Then
            // Expected: 5 + 1500 + 300 = 1805.00
            assertThat(result.getFinalPrice()).isEqualByComparingTo(new BigDecimal("1805.00"));
        }

        @Test
        @DisplayName("should correctly round using HALF_UP")
        void shouldRoundHalfUp() {
            // Given: Value that ends in .5
            PricingRequestDto request = PricingRequestDto.builder()
                    .orderId(9L)
                    .distanceKm(new BigDecimal("3.00"))  // 3 * 1.50 = 4.50
                    .durationMinutes(new BigDecimal("3.00"))  // 3 * 0.50 = 1.50
                    .build();

            // When
            PricingResponseDto result = pricingService.calculatePrice(request);

            // Then: 5.00 + 4.50 + 1.50 = 11.00
            assertThat(result.getFinalPrice()).isEqualByComparingTo(new BigDecimal("11.00"));
        }
    }
}
