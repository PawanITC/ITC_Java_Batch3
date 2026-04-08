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
 * It utilizes Idempotency Keys to prevent duplicate charges and leverages
 * Metadata to link Stripe objects back to internal FunKart database records.
 * </p>
 */
@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    /**
     * Creates a PaymentIntent in Stripe's system.
     * @param amount   Amount in the smallest currency unit (e.g., cents for USD).
     * @param currency 3-letter ISO currency code.
     * @param userId   Internal FunKart User ID for metadata.
     * @param paymentId Internal FunKart Payment Record ID for Idempotency.
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
     * Finalizes a payment that has been authorized by the client.
     */
    public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId, String returnUrl) throws StripeException {
        logger.info("Confirming Stripe Payment | PI: {}", paymentIntentId);

        return PaymentIntent.retrieve(paymentIntentId).confirm(
                PaymentIntentConfirmParams.builder()
                        .setPaymentMethod(paymentMethodId)
                        .setReturnUrl(returnUrl)
                        .build()
        );
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    /**
     * Initiates a full refund for a successful PaymentIntent.
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

    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
        logger.info("Cancelling Stripe Intent | PI: {}", paymentIntentId);
        return PaymentIntent.retrieve(paymentIntentId).cancel();
    }
}