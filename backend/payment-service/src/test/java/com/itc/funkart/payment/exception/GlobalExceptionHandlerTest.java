package com.itc.funkart.payment.exception;

import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>GlobalExceptionHandlerTest</h2>
 * Validates that the "Exception Shield" correctly transforms JVM exceptions
 * into the Unified ApiResponse format.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Should handle PaymentException - Domain Specific")
    void handlePaymentException() throws Exception {
        mockMvc.perform(get("/payment-ex"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.error.message").value("Limit reached"));
    }

    @Test
    @DisplayName("Should handle WebhookException - Verified Code Alignment")
    void handleWebhookException() throws Exception {
        mockMvc.perform(get("/webhook-ex"))
                .andExpect(status().isBadRequest())
                // Aligned with handler: WEBHOOK_VERIFICATION_FAILED
                .andExpect(jsonPath("$.error.code").value("WEBHOOK_VERIFICATION_FAILED"));
    }

    @Test
    @DisplayName("Should handle StripeException - 402 Payment Required")
    void handleStripeException() throws Exception {
        mockMvc.perform(get("/stripe-ex"))
                .andExpect(status().isPaymentRequired())
                // Aligned with handler: STRIPE_API_FAILURE
                .andExpect(jsonPath("$.error.code").value("STRIPE_API_FAILURE"));
    }

    @Test
    @DisplayName("Resilience: Should handle Rate Limiting (429)")
    void handleRateLimit() throws Exception {
        mockMvc.perform(get("/rate-limit-ex"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("Resilience: Should handle Circuit Breaker (503)")
    void handleCircuitBreaker() throws Exception {
        mockMvc.perform(get("/circuit-breaker-ex"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("CIRCUIT_OPEN"));
    }

    @Test
    @DisplayName("Should handle Generic Exception - Sanitized Output")
    void handleGenericException() throws Exception {
        mockMvc.perform(get("/generic-ex"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                // Aligned with exact handler string
                .andExpect(jsonPath("$.error.message").value("An unexpected system failure occurred in Payment Service"));
    }

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
            throw new CardException("Card Declined", "req_1", "declined", null, null, null, 402, null);
        }

        @GetMapping("/rate-limit-ex")
        public void throwRateLimit() {
            throw mock(RequestNotPermitted.class);
        }

        @GetMapping("/circuit-breaker-ex")
        public void throwCircuitBreaker() {
            throw mock(CallNotPermittedException.class);
        }

        @GetMapping("/generic-ex")
        public void throwGenericEx() {
            throw new RuntimeException("DB Connection Lost");
        }
    }
}