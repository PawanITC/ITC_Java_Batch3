package com.itc.funkart.payment.dto.webhook;

import com.itc.funkart.payment.exception.WebhookException;

import java.io.IOException;

/**
 * Mapper for converting raw Stripe webhook JSON to DTOs.
 */
public class PaymentIntentMapper {

    private PaymentIntentMapper() {
        /* This utility class should not be instantiated */
    }
    /**
     * Convert raw JSON payload to PaymentIntentWebhookDto.
     * @param json raw Stripe webhook JSON
     * @return PaymentIntentWebhookDto
     */
    public static PaymentIntentWebhookDto fromJson(String json) {
        try {
            return PaymentIntentWebhookDto.fromJson(json);
        } catch (IOException e) {
            throw new WebhookException("Failed to parse Stripe webhook payload: " + e.getMessage());
        }
    }
}