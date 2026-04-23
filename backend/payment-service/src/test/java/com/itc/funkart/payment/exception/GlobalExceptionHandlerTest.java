package com.itc.funkart.payment.exception;

import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>GlobalExceptionHandlerTest</h2>
 * <p>
 * This test suite validates the centralized exception handling logic of the Payment Service.
 * It ensures that various exceptions—ranging from domain-specific to generic system failures—
 * are correctly intercepted and transformed into a standardized {@code ApiResponse}.
 * </p>
 * * <h3>Testing Strategy:</h3>
 * <p>
 * Uses <b>Standalone MockMvc</b> setup. By manually registering the {@link GlobalExceptionHandler},
 * we isolate the advice logic from the full Spring Boot context, resulting in ultra-fast
 * unit tests that don't require external dependencies like Postgres or Kafka.
 * </p>
 *
 * @author Abbas
 * @version 2.1
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    /**
     * Initializes the MockMvc environment with a mock controller and the Global Exception Advice.
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * Verifies that {@link PaymentException} results in a 400 Bad Request
     * and preserves the domain-specific error code.
     */
    @Test
    @DisplayName("Should handle PaymentException with 400 and code")
    void handlePaymentException() throws Exception {
        mockMvc.perform(get("/payment-ex"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.error.message").value("Limit reached"));
    }

    /**
     * Verifies that {@link WebhookException} is correctly categorized under WEBHOOK_ERROR.
     */
    @Test
    @DisplayName("Should handle WebhookException with 400")
    void handleWebhookException() throws Exception {
        mockMvc.perform(get("/webhook-ex"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("WEBHOOK_ERROR"));
    }

    /**
     * Verifies that {@link StripeException} returns a 402 Payment Required status.
     * This is critical for frontend logic to prompt for a new payment method.
     */
    @Test
    @DisplayName("Should handle StripeException with 402 (Payment Required)")
    void handleStripeException() throws Exception {
        mockMvc.perform(get("/stripe-ex"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error.code").value("STRIPE_API_ERROR"));
    }

    /**
     * Validates handling of metadata mapping failures.
     */
    @Test
    @DisplayName("Should handle IntentMappingException with 400")
    void handleIntentMappingException() throws Exception {
        mockMvc.perform(get("/mapping-ex"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MAPPING_FAILED"));
    }

    /**
     * Validates handling of standard Java validation exceptions.
     */
    @Test
    @DisplayName("Should handle IllegalArgumentException with 400")
    void handleIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/illegal-ex"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    /**
     * Critical Security Test: Ensures that raw system exceptions (like DB errors)
     * are swallowed and replaced with a sanitized message for the client.
     */
    @Test
    @DisplayName("Should handle Generic Exception with 500 and sanitized message")
    void handleGenericException() throws Exception {
        mockMvc.perform(get("/generic-ex"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                // Message should be the sanitized version from the handler, not "Unexpected Database Failure"
                .andExpect(jsonPath("$.error.message").value("An unexpected system error occurred. Our engineering team has been notified."));
    }

    /**
     * Internal mock controller designed to trigger various exception scenarios
     * for verification by the Advice.
     */
    @RestController
    static class TestController {
        @GetMapping("/payment-ex")
        public void throwPaymentEx() {
            throw new PaymentException("Limit reached", "LIMIT_EXCEEDED");
        }

        @GetMapping("/webhook-ex")
        public void throwWebhookEx() {
            throw new WebhookException("Invalid Signature");
        }

        @GetMapping("/stripe-ex")
        public void throwStripeEx() throws StripeException {
            // Simulates a card decline or API failure from the Stripe SDK
            throw new CardException("Card Declined", "req_123", "declined", null, null, null, 402, null);
        }

        @GetMapping("/mapping-ex")
        public void throwMappingEx() {
            throw new IntentMappingException("Bad JSON Mapping");
        }

        @GetMapping("/illegal-ex")
        public void throwIllegalEx() {
            throw new IllegalArgumentException("Invalid Argument Provided");
        }

        @GetMapping("/generic-ex")
        public void throwGenericEx() {
            throw new RuntimeException("Unexpected Database Failure");
        }
    }
}