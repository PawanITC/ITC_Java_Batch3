package com.itc.funkart.payment.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO for Stripe PaymentIntent webhook payload.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentIntentWebhookDto(
        @JsonProperty("id") String id,
        @JsonProperty("status") String status,
        @JsonProperty("metadata") Map<String, String> metadata
) {}