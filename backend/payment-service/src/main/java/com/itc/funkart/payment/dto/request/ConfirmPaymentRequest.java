package com.itc.funkart.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for confirming a PaymentIntent with a specific Payment Method.
 * <p>
 * This is used in the "Manual Confirmation" flow where the frontend collects
 * card details via Stripe Elements and sends the resulting 'pm_xxx' ID
 * back to the server to finalize the charge.
 * </p>
 */
public record ConfirmPaymentRequest(
        @NotBlank(message = "Payment Intent ID is required")
        String paymentIntentId, // The 'pi_xxx' identifier

        @NotBlank(message = "Payment Method ID is required")
        String paymentMethodId,  // The 'pm_xxx' identifier from the frontend

        @NotBlank(message = "Return URL is required")
                String returnUrl
) {}