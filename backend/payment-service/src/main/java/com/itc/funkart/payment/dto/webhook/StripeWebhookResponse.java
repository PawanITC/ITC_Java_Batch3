package com.itc.funkart.payment.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standardized Response for Stripe Webhook events.
 */
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class StripeWebhookResponse {

    private String status;
    private String message;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // Custom 2-arg constructor for the "new" keyword
    public StripeWebhookResponse(String status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}