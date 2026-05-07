package com.itc.funkart.payment.service;

public final class StripeIdempotencyKeys {

    /**
     * Uses paymentId (DB row) instead of orderId so that a fresh database always
     * generates a new Stripe-side idempotency key, preventing IdempotencyException
     * on re-deployments where the DB is wiped but Stripe's 24-hour key window hasn't expired.
     */
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