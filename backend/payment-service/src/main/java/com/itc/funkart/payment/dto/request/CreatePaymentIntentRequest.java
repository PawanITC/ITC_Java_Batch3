package com.itc.funkart.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        @JsonProperty("orderId")
        @NotNull(message = "Order ID is required")
        Long orderId,

        @JsonProperty("amount")
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        Long amount,

        @JsonProperty("currency")
        @NotNull(message = "Currency is required")
        String currency
) {}