package com.itc.funkart.payment.controller;

import com.itc.funkart.payment.config.ApiConfig;
import com.itc.funkart.payment.exception.PaymentException;
import com.itc.funkart.payment.service.JwtService;
import com.itc.funkart.payment.service.PaymentService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>PaymentWebhookControllerTest</h2>
 * Validates the Stripe signature verification logic and service delegation.
 */
@WebMvcTest(PaymentWebhookController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private ApiConfig apiConfig;

    @MockitoBean
    private JwtService jwtService;

    private MockedStatic<Webhook> mockedWebhook;

    @BeforeEach
    void setUp() {
        // Essential: Initialize static mock for every test to prevent leakage
        mockedWebhook = mockStatic(Webhook.class);
    }

    @AfterEach
    void tearDown() {
        // Essential: Close to prevent "ThreadLocal" memory leaks in the JVM
        if (mockedWebhook != null) {
            mockedWebhook.close();
        }
    }

    private Event mockStripeEvent(StripeObject obj) {
        Event event = mock(Event.class, RETURNS_DEEP_STUBS);
        when(event.getType()).thenReturn("payment_intent.succeeded");
        when(event.getId()).thenReturn("evt_test_123");
        when(event.getDataObjectDeserializer().getObject()).thenReturn(Optional.of(obj));
        return event;
    }

    @Nested
    @DisplayName("Webhook Security & Verification")
    class SecurityTests {

        @Test
        @DisplayName("POST /webhook - 401 on Invalid Signature")
        void handleInvalidSignature() throws Exception {
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenThrow(new com.stripe.exception.SignatureVerificationException("Invalid Sig", "sig"));

            mockMvc.perform(post("/payments/webhook")
                            .header("Stripe-Signature", "invalid-header")
                            .content("{\"id\": \"evt_123\"}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("Unauthorized"));
        }

        @Test
        @DisplayName("POST /webhook - 400 on Missing Signature Header")
        void handleMissingHeader() throws Exception {
            mockMvc.perform(post("/payments/webhook")
                            .content("{}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Missing Stripe-Signature header"));
        }
    }

    @Nested
    @DisplayName("Webhook Event Processing")
    class ProcessingTests {

        @Test
        @DisplayName("POST /webhook - 200 on Successful Processing")
        void handleSucceededEvent() throws Exception {
            PaymentIntent intent = mock(PaymentIntent.class);
            Event event = mockStripeEvent(intent);

            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);

            mockMvc.perform(post("/payments/webhook")
                            .header("Stripe-Signature", "valid-sig")
                            .content("{}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("Success"))
                    .andExpect(jsonPath("$.message").value("Event processed: payment_intent.succeeded"));

            verify(paymentService, times(1)).processWebhookEvent(any(Event.class));
        }

        @Test
        @DisplayName("POST /webhook - 400 on PaymentException from Service")
        void handleServiceFailure() throws Exception {
            PaymentIntent intent = mock(PaymentIntent.class);
            Event event = mockStripeEvent(intent);

            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);

            // Simulate business logic failure (e.g., order not found in DB)
            doThrow(new PaymentException("Invalid Order Reference"))
                    .when(paymentService).processWebhookEvent(any(Event.class));

            mockMvc.perform(post("/payments/webhook")
                            .header("Stripe-Signature", "valid-sig")
                            .content("{}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid Order Reference"));
        }
    }
}