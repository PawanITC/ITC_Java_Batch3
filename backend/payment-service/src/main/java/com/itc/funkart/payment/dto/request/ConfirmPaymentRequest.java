package com.itc.funkart.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPaymentRequest(
        @NotBlank(message = "Payment Intent ID is required")
        String paymentIntentId,

        @NotBlank(message = "Payment Method ID is required")
        String paymentMethodId
) {}