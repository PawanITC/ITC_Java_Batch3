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

    // ================= CREATE PAYMENT INTENT =================
    public PaymentIntent createPaymentIntent(BigDecimal amount,
                                             String currency,
                                             Long userId,
                                             Long paymentId,
                                             String idempotencyKey) throws StripeException {

        long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, String> metadata = Map.of(
                "userId", String.valueOf(userId),
                "paymentId", String.valueOf(paymentId)
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
                .setReturnUrl("http://localhost:5173/")
                .build();

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build();

        return PaymentIntent.create(params, options);
    }

    // ================= CONFIRM PAYMENT =================
    public PaymentIntent confirmPaymentIntent(String paymentIntentId,
                                              String paymentMethodId) throws StripeException {

        logger.info("Confirming PaymentIntent {}", paymentIntentId);

        return PaymentIntent.retrieve(paymentIntentId).confirm(
                PaymentIntentConfirmParams.builder()
                        .setPaymentMethod(paymentMethodId)
                        .setReturnUrl("http://localhost:5173/")
                        .build()
        );
    }

    // ================= RETRIEVE =================
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    // ================= REFUND =================
    public void refundPayment(String paymentIntentId) throws StripeException {

        logger.info("Refunding PaymentIntent {}", paymentIntentId);

        Refund.create(
                RefundCreateParams.builder()
                        .setPaymentIntent(paymentIntentId)
                        .build()
        );
    }

    // ================= CANCEL =================
    public void cancelPaymentIntent(String paymentIntentId) throws StripeException {

        logger.info("Cancelling PaymentIntent {}", paymentIntentId);

        PaymentIntent.retrieve(paymentIntentId).cancel();
    }
}