package com.itc.funkart.payment.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.RefundCreateParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    private static final String DEFAULT_RETURN_URL = "http://localhost:5173/"; // Can be configured via properties

    // ================= CREATE PAYMENT INTENT =================
    public PaymentIntent createPaymentIntent(BigDecimal amount,
                                             String currency,
                                             Long userId,
                                             Long orderId,
                                             String idempotencyKey) throws StripeException {

        long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, String> metadata = Map.of(
                "userId", String.valueOf(userId),
                "orderId", String.valueOf(orderId)
        );

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency.toLowerCase())
                .putAllMetadata(metadata)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build();

        logger.info("Creating Stripe PaymentIntent for user {} and order {}", userId, orderId);

        return PaymentIntent.create(params, options);
    }

    // ================= CONFIRM PAYMENT =================
    public PaymentIntent confirmPaymentIntent(String paymentIntentId,
                                              String paymentMethodId) throws StripeException {

        logger.info("Confirming Stripe PaymentIntent {}", paymentIntentId);

        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);

        PaymentIntentConfirmParams confirmParams = PaymentIntentConfirmParams.builder()
                .setPaymentMethod(paymentMethodId)
                .setReturnUrl(DEFAULT_RETURN_URL)
                .build();

        return intent.confirm(confirmParams);
    }

    // ================= RETRIEVE PAYMENT INTENT =================
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    // ================= REFUND =================
    public void refundPayment(String paymentIntentId) throws StripeException {
        logger.info("Refunding Stripe PaymentIntent {}", paymentIntentId);

        RefundCreateParams refundParams = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .build();

        Refund.create(refundParams);
    }

    // ================= CANCEL =================
    public void cancelPaymentIntent(String paymentIntentId) throws StripeException {
        logger.info("Cancelling Stripe PaymentIntent {}", paymentIntentId);

        PaymentIntent.retrieve(paymentIntentId).cancel();
    }
}