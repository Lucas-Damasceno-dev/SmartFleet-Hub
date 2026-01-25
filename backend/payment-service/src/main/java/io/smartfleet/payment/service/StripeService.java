package io.smartfleet.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Stripe payment integration service.
 * Mock implementation for development - replace with actual Stripe SDK in production.
 */
@Service
@Slf4j
public class StripeService {

    @Value("${payment.stripe.secret-key:sk_test_placeholder}")
    private String secretKey;

    /**
     * Create a payment intent (mock implementation).
     *
     * @param orderId Order ID
     * @param amount Amount in decimal (e.g., 15.50)
     * @param currency Currency code (e.g., "brl")
     * @return Payment intent ID
     */
    public String createPaymentIntent(Long orderId, BigDecimal amount, String currency) {
        log.info("Creating payment intent for order {} - amount: {} {}", orderId, amount, currency);

        if (isMockMode()) {
            String mockIntentId = "pi_mock_" + System.currentTimeMillis() + "_" + orderId;
            log.info("MOCK MODE: Created payment intent {}", mockIntentId);
            return mockIntentId;
        }

        // TODO: Implement actual Stripe integration
        // Stripe.apiKey = secretKey;
        // PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
        //     .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
        //     .setCurrency(currency)
        //     .putMetadata("order_id", orderId.toString())
        //     .build();
        // PaymentIntent intent = PaymentIntent.create(params);
        // return intent.getId();

        throw new UnsupportedOperationException("Stripe integration not implemented");
    }

    /**
     * Confirm a payment (mock implementation).
     *
     * @param paymentIntentId Payment intent ID
     * @return true if payment confirmed
     */
    public boolean confirmPayment(String paymentIntentId) {
        log.info("Confirming payment intent: {}", paymentIntentId);

        if (isMockMode()) {
            log.info("MOCK MODE: Payment confirmed for {}", paymentIntentId);
            return true;
        }

        // TODO: Implement actual Stripe confirmation
        throw new UnsupportedOperationException("Stripe integration not implemented");
    }

    /**
     * Refund a payment (mock implementation).
     *
     * @param paymentIntentId Payment intent ID
     * @param amount Amount to refund
     * @return Refund ID
     */
    public String refundPayment(String paymentIntentId, BigDecimal amount) {
        log.info("Refunding {} from payment intent {}", amount, paymentIntentId);

        if (isMockMode()) {
            String mockRefundId = "re_mock_" + System.currentTimeMillis();
            log.info("MOCK MODE: Created refund {}", mockRefundId);
            return mockRefundId;
        }

        // TODO: Implement actual Stripe refund
        throw new UnsupportedOperationException("Stripe integration not implemented");
    }

    private boolean isMockMode() {
        return secretKey == null || 
               secretKey.isBlank() || 
               secretKey.contains("placeholder") || 
               secretKey.startsWith("sk_test");
    }
}
