package io.smartfleet.payment.controller;

import io.smartfleet.common.dto.ApiResponse;
import io.smartfleet.common.dto.PricingRequestDto;
import io.smartfleet.common.dto.PricingResponseDto;
import io.smartfleet.payment.service.PricingService;
import io.smartfleet.payment.service.StripeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller for payment and pricing endpoints.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PricingService pricingService;
    private final StripeService stripeService;

    @PostMapping("/calculate-price")
    public ApiResponse<PricingResponseDto> calculatePrice(@Valid @RequestBody PricingRequestDto request) {
        PricingResponseDto response = pricingService.calculatePrice(request);
        return ApiResponse.of(response);
    }

    @PostMapping("/orders/{orderId}/payment-intent")
    public ApiResponse<PaymentIntentResponse> createPaymentIntent(
            @PathVariable Long orderId,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "brl") String currency) {
        
        String intentId = stripeService.createPaymentIntent(orderId, amount, currency);
        return ApiResponse.of(PaymentIntentResponse.builder()
                .paymentIntentId(intentId)
                .orderId(orderId)
                .amount(amount)
                .currency(currency)
                .build());
    }

    @PostMapping("/confirm/{paymentIntentId}")
    public ApiResponse<Boolean> confirmPayment(@PathVariable String paymentIntentId) {
        boolean confirmed = stripeService.confirmPayment(paymentIntentId);
        return ApiResponse.of(confirmed);
    }

    @PostMapping("/refund/{paymentIntentId}")
    public ApiResponse<RefundResponse> refundPayment(
            @PathVariable String paymentIntentId,
            @RequestParam BigDecimal amount) {
        
        String refundId = stripeService.refundPayment(paymentIntentId, amount);
        return ApiResponse.of(RefundResponse.builder()
                .refundId(refundId)
                .amount(amount)
                .build());
    }

    // Response DTOs
    @lombok.Data
    @lombok.Builder
    public static class PaymentIntentResponse {
        private String paymentIntentId;
        private Long orderId;
        private BigDecimal amount;
        private String currency;
    }

    @lombok.Data
    @lombok.Builder
    public static class RefundResponse {
        private String refundId;
        private BigDecimal amount;
    }
}
