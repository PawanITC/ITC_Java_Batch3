package com.itc.funkart.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);
    private static boolean initialized = false;

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        setApiKey(stripeApiKey);
    }

    /**
     * Static-safe method to set Stripe API key once
     */
    public static synchronized void setApiKey(String key) {
        if (!initialized) {
            Stripe.apiKey = key;
            initialized = true;
            logger.info("✓ Stripe API initialized (static-safe)");
        }
    }

    // ================= STATIC WRAPPERS =================
    public static PaymentIntent createPaymentIntentStatic(BigDecimal amount, String currency, Long userId, Long paymentId) throws StripeException {
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
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                )
                .build();

        return PaymentIntent.create(params);
    }

    public static PaymentIntent confirmPaymentIntentStatic(String paymentIntentId, String paymentMethodId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId).confirm(
                PaymentIntentConfirmParams.builder()
                        .setPaymentMethod(paymentMethodId)
                        .build()
        );
    }

    public static PaymentIntent retrievePaymentIntentStatic(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    public static void refundPaymentStatic(String paymentIntentId) throws StripeException {
        Refund.create(RefundCreateParams.builder().setPaymentIntent(paymentIntentId).build());
    }

    public static void cancelPaymentIntentStatic(String paymentIntentId) throws StripeException {
        PaymentIntent.retrieve(paymentIntentId).cancel();
    }
}