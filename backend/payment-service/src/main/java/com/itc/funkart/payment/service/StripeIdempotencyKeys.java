package com.itc.funkart.payment.service;

public final class StripeIdempotencyKeys {

    public static String createPaymentIntent(Long paymentId) {
        return "pi:create:payment:" + paymentId;
    }

    public static String confirmPaymentIntent(String paymentIntentId, Long paymentId) {
        return "pi:confirm:" + paymentIntentId + ":payment:" + paymentId;
    }

    public static String refund(Long paymentId, String stripePiId) {
        return "refund:payment:" + paymentId + ":pi:" + stripePiId;
    }
}
