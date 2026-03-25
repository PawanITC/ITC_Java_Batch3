package com.itc.funkart.payment.dto.response;

public record PaymentIntentResponse(
        String clientSecret,      // For frontend Stripe.js integration
        String paymentIntentId,   // Stripe payment intent ID
        String status             // "pending", "succeeded", "requires_action", etc
) {}