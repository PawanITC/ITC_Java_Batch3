package com.itc.funkart.payment.integration;

import com.itc.funkart.payment.config.SecurityConfig;
import com.itc.funkart.payment.dto.jwt.JwtUserDto;
import com.itc.funkart.payment.repository.PaymentRepository;
import com.itc.funkart.payment.service.JwtService;
import com.itc.funkart.payment.service.StripeService;
import com.stripe.model.PaymentIntent;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>Payment Service Integration Suite</h2>
 * <p>
 * This suite validates the orchestration between the Web Layer (MockMvc),
 * the Security Layer (JWT), the Business Layer (PaymentService),
 * and the Persistence Layer (Hibernate/H2).
 * </p>
 *
 * <b>Testing Strategy:</b>
 * <ul>
 * <li>Internal services and repositories are real (Spring Context).</li>
 * <li>External API (Stripe) is mocked via {@link MockitoBean}.</li>
 * <li>Database state is reset per method using {@link Transactional}.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude = {UserDetailsServiceAutoConfiguration.class})
public class PaymentIntegrationTest {

    private final String CREATE_INTENT_PATH = "/payments/create-intent";
    private final String WEBHOOK_PATH = "/payments/webhook";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private StripeService stripeService;
    private String validToken;

    @BeforeEach
    void setUp() {
        JwtUserDto mockUser = JwtUserDto.builder()
                .id(1L)
                .name("Abbas")
                .email("abbas@example.com")
                .role("ROLE_USER")
                .build();
        validToken = jwtService.generateJwtToken(mockUser);
    }

    @Nested
    @DisplayName("💳 Payment Intent Creation")
    class CreateIntentTests {

        /**
         * Verifies that a valid request results in a persisted entity and a
         * successful response containing Stripe's client secret.
         */
        @Test
        @Transactional
        @DisplayName("✅ Should persist payment and return client secret")
        void shouldSucceedHappyPath() throws Exception {
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_mock_789");
            when(mockIntent.getClientSecret()).thenReturn("secret_mock_789");
            when(mockIntent.getStatus()).thenReturn("requires_payment_method");

            doReturn(mockIntent).when(stripeService)
                    .createPaymentIntent(any(), any(), any(), any());

            String requestBody = """
                    {
                        "amount": 1500,
                        "currency": "usd",
                        "orderId": 101
                    }
                    """;

            mockMvc.perform(post(CREATE_INTENT_PATH)
                            .header("Authorization", "Bearer " + validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.clientSecret").value("secret_mock_789"));

            assertEquals(1, paymentRepository.count());
        }

        /**
         * Validates that Stripe failures are caught and mapped to 400 Bad Request
         * via the @ResponseStatus annotation on PaymentException.
         */
        @Test
        @DisplayName("❌ Should return 400 when Stripe API fails")
        void shouldReturn400OnStripeError() throws Exception {
            doThrow(new RuntimeException("Stripe Connection Refused"))
                    .when(stripeService).createPaymentIntent(any(), any(), any(), any());

            String requestBody = "{\"amount\": 1000, \"currency\": \"usd\", \"orderId\": 202}";

            mockMvc.perform(post(CREATE_INTENT_PATH)
                            .header("Authorization", "Bearer " + validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("PAYMENT_PROCESSING_ERROR"));
        }
    }

    @Nested
    @DisplayName("🛡️ Security & Validation")
    class SecurityValidationTests {

        @Test
        @DisplayName("🚫 Should reject requests without valid JWT")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post(CREATE_INTENT_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("⚠️ Should handle validation failure (500 fallback)")
        void shouldHandleInvalidInput() throws Exception {
            // Negative amount triggers @Positive validation
            String invalidJson = "{\"amount\": -1, \"currency\": \"usd\", \"orderId\": 303}";

            mockMvc.perform(post(CREATE_INTENT_PATH)
                            .header("Authorization", "Bearer " + validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));
        }
    }

    @Nested
    @DisplayName("🪝 Webhook Integration")
    class WebhookTests {

        @Test
        @DisplayName("🌍 Should allow public access to Webhook endpoint")
        void webhookShouldBePublic() throws Exception {
            // We expect a 400 here not because of Security, but because the
            // payload is missing the Stripe-Signature header, proving the request
            // actually reached the Controller logic.
            mockMvc.perform(post(WEBHOOK_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}