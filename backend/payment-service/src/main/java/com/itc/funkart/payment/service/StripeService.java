package com.itc.funkart.payment.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * <h2>StripeService</h2>
 * <p>
 * Low-level interface for the Stripe Java SDK. This service handles all
 * network communication with Stripe's cloud API.
 * </p>
 * <p>
 * <b>Key Features:</b>
 * <ul>
 * <li><b>Idempotency:</b> Prevents duplicate charges during network retries.</li>
 * <li><b>Metadata:</b> Attaches internal IDs to Stripe objects for auditing.</li>
 * <li><b>Automatic Payment Methods:</b> Enabled by default for flexibility.</li>
 * </ul>
 * </p>
 */
@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    /**
     * Creates a PaymentIntent in the Stripe system.
     * <p>
     * Uses an Idempotency Key tied to the local {@code paymentId} to ensure
     * that exactly one intent is created even if the request is retried.
     * </p>
     *
     * @param amount    The transaction amount in the smallest currency unit (cents).
     * @param currency  3-character ISO currency code.
     * @param userId    Internal user ID for metadata tagging.
     * @param paymentId Local database ID for metadata and idempotency.
     * @return The newly created {@link PaymentIntent}.
     * @throws StripeException if communication with Stripe fails.
     */
    public PaymentIntent createPaymentIntent(Long amount, String currency, Long userId, Long paymentId) throws StripeException {
        logger.debug("Preparing Stripe PaymentIntent | User: {} | Amount: {}", userId, amount);

        Map<String, String> metadata = Map.of(
                "userId", String.valueOf(userId),
                "paymentId", String.valueOf(paymentId)
        );

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency.toLowerCase())
                .putAllMetadata(metadata)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("pi-idempotency-" + paymentId)
                .build();

        return PaymentIntent.create(params, options);
    }

    /**
     * Confirms and finalizes a PaymentIntent.
     *
     * @param paymentIntentId The unique ID of the intent (pi_...).
     * @param paymentMethodId The payment method to charge (pm_...).
     * @param returnUrl       The URL Stripe redirects to after 3DS authentication.
     * @return The updated {@link PaymentIntent}.
     * @throws StripeException if the confirmation is rejected.
     */
    public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId, String returnUrl) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId)
                .confirm(PaymentIntentConfirmParams.builder()
                        .setPaymentMethod(paymentMethodId)
                        .setReturnUrl(returnUrl)
                        .build()
                );
    }

    /**
     * Requests a full refund for a specific PaymentIntent.
     *
     * @param paymentIntentId The ID of the intent to refund.
     * @return The resulting {@link Refund} object from Stripe.
     * @throws StripeException if the refund is disallowed.
     */
    public Refund refundPayment(String paymentIntentId) throws StripeException {
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .build();
        return Refund.create(params);
    }

    /**
     * Voids an uncaptured PaymentIntent.
     *
     * @param paymentIntentId The ID of the intent to cancel.
     * @return The cancelled intent.
     * @throws StripeException if the intent is in a terminal state.
     */
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId).cancel();
    }
}