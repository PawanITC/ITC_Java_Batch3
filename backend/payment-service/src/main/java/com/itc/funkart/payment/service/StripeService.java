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
 * Direct interface for the Stripe Java SDK.
 * <p>
 * This service handles the low-level API calls to Stripe's cloud servers.
 * <p>
 * It utilizes Idempotency Keys to prevent duplicate charges and leverages
 * Metadata to link Stripe objects back to internal FunKart database records.
 * <p>
 * </p>
 */
@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    /**
     * Creates a PaymentIntent in Stripe's system with built-in idempotency.
     * <p>
     * Uses an Idempotency Key based on the internal {@code paymentId} to ensure that retries
     * do not result in duplicate intents. Attaches {@code userId} and {@code paymentId}
     * to Stripe metadata for auditing.
     * </p>
     *
     * @param amount    Amount in cents (e.g., 1000 for $10.00).
     * @param currency  3-letter ISO code (e.g., "usd").
     * @param userId    Internal ID of the customer.
     * @param paymentId Internal ID used for the {@code idempotencyKey}.
     * @return The created {@link PaymentIntent}.
     * @throws StripeException if the API request fails.
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

        // Idempotency key ensures that even if a network retry happens,
        // Stripe won't create two different intents for the same internal record.
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("pi-idempotency-" + paymentId)
                .build();

        PaymentIntent intent = PaymentIntent.create(params, options);
        logger.info("Stripe Intent Created | PI: {} | Status: {}", intent.getId(), intent.getStatus());
        return intent;
    }

    /**
     * Finalizes an authorized payment using a specific PaymentMethod.
     * <p>
     * This method transitions the intent to a terminal state (succeeded) or
     * triggers {@code requires_action} if the bank requires 3D Secure.
     * </p>
     *
     * @param paymentIntentId The {@code pi_XXX} ID from Stripe.
     * @param paymentMethodId The {@code pm_XXX} ID representing the card/source.
     * @param returnUrl       The redirect URL for post-authentication (3DS).
     * @return The confirmed {@link PaymentIntent}.
     * @throws StripeException if confirmation is rejected by the processor.
     */
    public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId, String returnUrl) throws StripeException {
        logger.info("Confirming Stripe Payment | PI: {}", paymentIntentId);

        return PaymentIntent.retrieve(paymentIntentId)
                .confirm(PaymentIntentConfirmParams.builder()
                        .setPaymentMethod(paymentMethodId)
                        .setReturnUrl(returnUrl)
                        .build()
        );
    }

    /**
     * Retrieves the latest state of a specific PaymentIntent.
     * * @param paymentIntentId The {@code pi_XXX} identifier.
     * @return The {@link PaymentIntent} object fetched from Stripe.
     * @throws StripeException if the intent is not found.
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    /**
     * Initiates a full refund for a previously successful PaymentIntent.
     * <p>
     * This creates a new {@link Refund} object. The original PaymentIntent
     * will remain {@code succeeded} but will reflect the refunded amount.
     * </p>
     *
     * @param paymentIntentId The ID of the intent to be refunded.
     * @return The generated {@link Refund} record.
     * @throws StripeException if the refund is disallowed (e.g., already refunded).
     */
    public Refund refundPayment(String paymentIntentId) throws StripeException {
        logger.warn("Initiating Stripe Refund | PI: {}", paymentIntentId);

        Refund refund = Refund.create(
                RefundCreateParams.builder()
                        .setPaymentIntent(paymentIntentId)
                        .build()
        );

        logger.info("Stripe Refund Success | RefundID: {} | PI: {}", refund.getId(), paymentIntentId);
        return refund;
    }

    /**
     * Voids a PaymentIntent that has not yet been captured.
     * <p>
     * Useful for cleaning up abandoned checkouts or user cancellations
     * before the funds are actually moved.
     * </p>
     *
     * @param paymentIntentId The ID of the intent to cancel.
     * @return The cancelled {@link PaymentIntent}.
     * @throws StripeException if the intent is already in a final state.
     */
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
        logger.info("Cancelling Stripe Intent | PI: {}", paymentIntentId);
        return PaymentIntent.retrieve(paymentIntentId).cancel();
    }
}