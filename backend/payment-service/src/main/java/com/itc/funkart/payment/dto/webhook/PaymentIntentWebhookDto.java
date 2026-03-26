package com.itc.funkart.payment.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Minimal DTO for Stripe PaymentIntent webhook events.
 */
public record PaymentIntentWebhookDto(
        @JsonProperty("id") String id,
        @JsonProperty("status") String status
) {

    /**
     * Convert raw JSON payload into DTO.
     */
    public static PaymentIntentWebhookDto fromJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, PaymentIntentWebhookDto.class);
    }
}