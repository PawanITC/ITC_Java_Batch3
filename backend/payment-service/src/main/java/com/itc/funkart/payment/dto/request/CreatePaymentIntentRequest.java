package com.itc.funkart.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for initiating a new Stripe PaymentIntent.
 * <p>
 * DESIGN NOTES:
 * 1. <b>Cents-Only:</b> The 'amount' field is a Long to match Stripe's requirement
 * for the smallest currency unit. This prevents floating-point rounding errors.
 * 2. <b>Validation:</b> Uses @Positive to ensure we never attempt to charge
 * a zero or negative balance.
 * </p>
 */
public record CreatePaymentIntentRequest(
        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        Long amount, // Represented in cents (e.g., 100 for $1.00)

        @NotNull(message = "Currency is required")
        String currency // ISO code (e.g., "usd")
) {}