package com.itc.funkart.payment.dto.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.payment.exception.IntentMappingException;

/**
 * Mapper helper to convert raw JSON into PaymentIntentWebhookDto.
 */
public class PaymentIntentMapper {
    private PaymentIntentMapper() {
        /* This utility class should not be instantiated */
    }
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static PaymentIntentWebhookDto fromJson(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentIntentWebhookDto.class);
        } catch (Exception ex) {
            throw new IntentMappingException("Failed to map Stripe webhook JSON to DTO");
        }
    }
}