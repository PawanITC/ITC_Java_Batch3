package com.itc.funkart.payment.service;

public final class StripeIdempotencyKeys {

    public static String createPaymentIntent(Long orderId) {
        return "pi:create:order:" + orderId;
    }

    public static String confirmPaymentIntent(String paymentIntentId, Long paymentId) {
        return "pi:confirm:" + paymentIntentId + ":payment:" + paymentId;
    }

    public static String refund(Long paymentId, String stripePiId) {
        return "refund:payment:" + paymentId + ":pi:" + stripePiId;
    }
}