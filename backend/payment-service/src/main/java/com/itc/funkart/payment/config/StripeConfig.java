package com.itc.funkart.payment.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${stripe.api-key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        com.stripe.Stripe.apiKey = apiKey; // Stripe.apiKey is static
    }
}