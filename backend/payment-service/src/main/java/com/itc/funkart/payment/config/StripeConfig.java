package com.itc.funkart.payment.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Global Configuration for the Stripe Java SDK.
 * <p>
 * This class initializes the Stripe API context by setting the secret key
 * globally. This ensures that all downstream service calls (StripeService)
 * are authenticated without needing to pass the key manually.
 * </p>
 * * @author Abbas (Funkart Team)
 */
@Configuration
public class StripeConfig {

    @Value("${stripe.api-key}")
    private String apiKey;

    /**
     * Initializes the Stripe SDK with the injected API Key.
     * Uses @PostConstruct to ensure initialization happens exactly once
     * during the application startup phase.
     */
    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Stripe API Key is missing! Check your configuration.");
        }
        com.stripe.Stripe.apiKey = this.apiKey;
    }
}