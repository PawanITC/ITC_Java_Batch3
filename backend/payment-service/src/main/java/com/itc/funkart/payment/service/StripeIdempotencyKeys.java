package com.itc.funkart.payment.service;

import java.util.UUID;

public final class StripeIdempotencyKeys {

    private StripeIdempotencyKeys() {
    }

    // IMPORTANT: include orderId to guarantee uniqueness per business flow
    public static String createPaymentIntent(Long paymentId, Long orderId) {
        return "pi:create:order:" + orderId + ":payment:" + paymentId;
    }

    public static String confirmPaymentIntent(String paymentIntentId, Long paymentId) {
        return "pi:confirm:" + paymentIntentId + ":payment:" + paymentId;
    }

    public static String refund(Long paymentId, String stripePiId) {
        return "refund:payment:" + paymentId + ":pi:" + stripePiId;
    }

    // optional safety escape hatch for retries
    public static String safeRandomSuffix() {
        return UUID.randomUUID().toString();
    }
}