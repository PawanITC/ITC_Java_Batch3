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
 * Low-level interface for the Stripe Java SDK. This service encapsulates all
 * network communication with Stripe's cloud infrastructure, acting as a stateless
 * wrapper within the FunKart Payment Microservice.
 * </p>
 *
 * <p><b>Key Architectural Patterns:</b></p>
 * <ul>
 *   <li><b>Idempotency:</b> Prevents duplicate charges by using unique keys tied to local database records.</li>
 *   <li><b>Observability:</b> Implements SLF4J logging to track outgoing API requests and performance.</li>
 *   <li><b>Security:</b> Attaches internal user and payment IDs via metadata for cloud-side auditing.</li>
 * </ul>
 *
 * <p><b>JVM Note:</b> This service is a Singleton. Its methods are thread-safe
 * and rely on the stack for local variable storage to minimize heap pressure.</p>
 */
@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    /**
     * Creates a PaymentIntent in the Stripe system.
     * <p>
     * This is the first step in the payment lifecycle. It signals to Stripe that a user
     * intends to pay a specific amount. An idempotency key is used to ensure that network
     * retries do not result in multiple Intents for the same Order.
     * </p>
     *
     * @param amount    Transaction amount in the smallest currency unit (e.g., cents for USD).
     * @param currency  3-character ISO currency code (e.g., "usd").
     * @param userId    Internal ID of the user initiating the payment for metadata tagging.
     * @param paymentId Local database ID used for metadata and the idempotency key.
     * @return A {@link PaymentIntent} object containing the {@code client_secret} for the frontend.
     * @throws StripeException If the request fails due to network issues, invalid parameters, or API limits.
     */
    public PaymentIntent createPaymentIntent(
            Long amount,
            String currency,
            Long userId,
            Long paymentId,
            Long orderId
    ) throws StripeException {

        logger.debug("Requesting Stripe Intent | orderId={} paymentId={} amount={}",
                orderId, paymentId, amount);

        Map<String, String> metadata = Map.of(
                "userId", String.valueOf(userId),
                "paymentId", String.valueOf(paymentId),
                "orderId", String.valueOf(orderId)
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
                .setIdempotencyKey(
                        StripeIdempotencyKeys.createPaymentIntent(paymentId, orderId)
                )
                .build();

        return PaymentIntent.create(params, options);
    }

    /**
     * Confirms and finalizes a PaymentIntent.
     * <p>
     * Used in flows requiring server-side confirmation, such as manual captures or
     * secondary authentication (3D Secure).
     * </p>
     *
     * @param piId      The Stripe PaymentIntent ID (pi_...).
     * @param pmId      The Stripe PaymentMethod ID (pm_...) provided by the frontend.
     * @param returnUrl The URL to redirect the user back to after 3DS verification.
     * @throws StripeException If the payment is declined or the ID is invalid.
     */
    public void confirmPaymentIntent(
            String piId,
            String pmId,
            String returnUrl,
            Long paymentId
    ) throws StripeException {

        logger.info("Confirming Stripe Intent | PI={} paymentId={}", piId, paymentId);

        PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                .setPaymentMethod(pmId)
                .setReturnUrl(returnUrl)
                .build();

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(
                        StripeIdempotencyKeys.confirmPaymentIntent(piId, paymentId))
                .build();

        PaymentIntent.retrieve(piId).confirm(params, options);
    }

    /**
     * Requests a full refund for a specific PaymentIntent.
     * <p>
     * This operation is idempotent. If a refund has already been requested for
     * this Intent ID, Stripe will return the existing refund object rather than
     * creating a new one.
     * </p>
     *
     * @param piId The Stripe PaymentIntent ID (pi_...) associated with the successful charge.
     * @return The {@link Refund} object detailing the reversal.
     * @throws StripeException If the payment is not in a refundable state (e.g., already refunded).
     */
    public Refund refundPayment(String piId, Long paymentId) throws StripeException {

        logger.warn("Processing Stripe Refund | paymentId={} piId={}", paymentId, piId);

        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(piId)
                .build();

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(
                        StripeIdempotencyKeys.refund(paymentId, piId)
                )
                .build();

        return Refund.create(params, options);
    }


    /**
     * Voids an uncaptured PaymentIntent.
     * <p>
     * Used to cancel a transaction before funds are moved. Note that captured
     * payments must use {@link #refundPayment(String, Long)}} instead.
     * </p>
     *
     * @param piId The ID of the intent to cancel.
     * @throws StripeException If the intent is already in a terminal state (succeeded/cancelled).
     */
    public void cancelPaymentIntent(String piId) throws StripeException {
        logger.info("Cancelling Stripe Intent | PI={}", piId);
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("pi:cancel:payment-intent:" + piId)
                .build();

        PaymentIntent.retrieve(piId).cancel(options);
    }
}