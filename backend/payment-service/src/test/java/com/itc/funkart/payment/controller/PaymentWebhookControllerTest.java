package com.itc.funkart.payment.controller;

import com.itc.funkart.payment.config.ApiConfig;
import com.itc.funkart.payment.service.JwtService;
import com.itc.funkart.payment.service.PaymentService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h1>PaymentWebhookControllerTest</h1>
 * <p>
 * This test suite validates the Stripe Webhook integration. Unlike standard controllers,
 * Webhooks require static mocking of the Stripe SDK to simulate cryptographic signature
 * verification and payload deserialization.
 * </p>
 *
 * <h3>Key Testing Areas:</h3>
 * <ul>
 * <li><b>Security:</b> Ensuring {@code SignatureVerificationException} results in a 401 Unauthorized.</li>
 * <li><b>Integrity:</b> Verifying that corrupted or empty Stripe objects result in a 400 Bad Request.</li>
 * <li><b>Versioning:</b> Confirming the webhook endpoint is reachable under the {@code /api/v1} prefix.</li>
 * </ul>
 *
 * @author Abbas
 * @version 1.0
 */
@WebMvcTest(PaymentWebhookController.class)
@AutoConfigureMockMvc(addFilters = false) // Webhooks are validated by Stripe signatures, not JWTs
class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Mocked service to handle the business logic after a successful webhook.
     */
    @MockitoBean
    private PaymentService paymentService;

    /**
     * Infrastructure mocks required for WebConfig and Context loading.
     */
    @MockitoBean
    private ApiConfig apiConfig;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;
    @MockitoBean
    private RestClient restClient;

    /**
     * Static mock for the Stripe {@link Webhook} utility.
     * Must be closed in {@code tearDown} to prevent memory leaks in the test runner.
     */
    private MockedStatic<Webhook> mockedWebhook;

    /**
     * Sets up the static mock for Stripe's Webhook utility and configures the API versioning prefix.
     */
    @BeforeEach
    void setUp() {
        mockedWebhook = mockStatic(Webhook.class);
        when(apiConfig.getVersion()).thenReturn("api/v1");
    }

    /**
     * Closes the static mock context after every test to ensure isolation.
     */
    @AfterEach
    void tearDown() {
        if (mockedWebhook != null) {
            mockedWebhook.close();
        }
    }

    /**
     * <h2>Stripe Webhook: Successful Events</h2>
     * Validates flows where the signature is valid and the payload is correctly structured.
     */
    @Nested
    @DisplayName("Stripe Webhook: Successful Events")
    class WebhookSuccessTests {

        /**
         * Verifies that a {@code payment_intent.succeeded} event is processed and returns HTTP 200.
         * * @throws Exception if the MockMvc request fails
         */
        @Test
        @DisplayName("Process payment_intent.succeeded")
        void handleSucceededEvent() throws Exception {
            // Arrange
            Event mockEvent = mock(Event.class);
            PaymentIntent mockPi = mock(PaymentIntent.class);
            EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

            when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
            when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
            when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPi));

            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            // Act & Assert
            // Path updated to include /api/v1 prefix from WebConfig
            mockMvc.perform(post("/api/v1/payments/webhook")
                            .header("Stripe-Signature", "valid_sig")
                            .content("payload_content")
                            .contentType(MediaType.TEXT_PLAIN))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Event processed: payment_intent.succeeded"));

            verify(paymentService, times(1)).handlePaymentSuccess(any(PaymentIntent.class));
        }
    }

    /**
     * <h2>Stripe Webhook: Security & Failures</h2>
     * Validates that the system correctly rejects unauthorized or malformed requests.
     */
    @Nested
    @DisplayName("Stripe Webhook: Security & Failures")
    class WebhookFailureTests {

        /**
         * Verifies that requests with invalid signatures are rejected with 401 Unauthorized.
         */
        @Test
        @DisplayName("Reject Invalid Signature (401)")
        void handleInvalidSignature() throws Exception {
            // Arrange
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenThrow(new com.stripe.exception.SignatureVerificationException("Invalid Signature", "sig_header"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/payments/webhook")
                            .header("Stripe-Signature", "fake_sig")
                            .content("malicious_payload"))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * Verifies that if Stripe sends a valid signature but the object cannot be extracted,
         * the system returns a 400 Bad Request.
         */
        @Test
        @DisplayName("Handle Deserialization Error (400)")
        void handleNullObject() throws Exception {
            // Arrange
            Event mockEvent = mock(Event.class);
            EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

            when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
            when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
            when(mockDeserializer.getObject()).thenReturn(Optional.empty());

            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            // Act & Assert
            mockMvc.perform(post("/api/v1/payments/webhook")
                            .header("Stripe-Signature", "sig")
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    /**
     * <h2>Stripe Webhook: Coverage Boosters</h2>
     * <p>This nested class contains tests specifically designed to target complex
     * branching logic, including switch-case defaults and nested exception handling.</p>
     */
    @Nested
    @DisplayName("Stripe Webhook: Coverage Boosters")
    class WebhookCoverageTests {

        /**
         * <b>Scenario:</b> Payment failure notification from Stripe.<br>
         * <b>Coverage:</b> Line 74-80 (payment_intent.payment_failed case).<br>
         * <b>Technical Note:</b> Mocks a {@code StripeError} to ensure the logger can
         * access {@code getMessage()} without a NullPointerException.
         */
        @Test
        @DisplayName("Process payment_intent.payment_failed - Should trigger failure handling logic")
        void handleFailedEvent() throws Exception {
            Event mockEvent = mock(Event.class);
            PaymentIntent mockPi = mock(PaymentIntent.class);
            EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
            // Mocking the error message for the logger line 77
            com.stripe.model.StripeError mockError = mock(com.stripe.model.StripeError.class);
            when(mockError.getMessage()).thenReturn("Card Declined");
            when(mockPi.getLastPaymentError()).thenReturn(mockError);
            when(mockPi.getId()).thenReturn("pi_failed_123");

            when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
            when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
            when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPi));

            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            mockMvc.perform(post("/api/v1/payments/webhook")
                            .header("Stripe-Signature", "valid")
                            .content("payload"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("Success"));

            verify(paymentService).handlePaymentFailure(any());
        }

        /**
         * <b>Scenario:</b> Critical failure during Stripe object deserialization.<br>
         * <b>Coverage:</b> Line 58 (RuntimeException) and Line 93-95 (General Catch block).<br>
         * <b>Technical Note:</b> Simulates a failure in {@code deserializeUnsafe()} to trigger
         * the internal {@code RuntimeException}, which is then caught by the global handler.
         */
        @Test
        @DisplayName("Trigger Deserialization RuntimeException - Handle critical failures during Stripe payload extraction")
        void handleDeserializationException() throws Exception {
            Event mockEvent = mock(Event.class);
            EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

            when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
            // Force orElseGet to trigger
            when(mockDeserializer.getObject()).thenReturn(Optional.empty());
            // Force deserializeUnsafe to throw the specific Stripe exception
            when(mockDeserializer.deserializeUnsafe()).thenThrow(new com.stripe.exception.EventDataObjectDeserializationException("Bad Data", "json"));

            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            // This hits the 'catch (Exception ex)' at line 93 because of the RuntimeException at line 58
            mockMvc.perform(post("/api/v1/payments/webhook")
                            .header("Stripe-Signature", "valid")
                            .content("payload"))
                    .andExpect(status().isInternalServerError());
        }

        /**
         * <b>Scenario:</b> Stripe sends an event type not explicitly handled by the switch.<br>
         * <b>Coverage:</b> Line 81 (Default switch case).<br>
         * <b>Technical Note:</b> Uses {@code customer.created} to verify the "Ignored event type"
         * logging logic.
         */
        @Test
        @DisplayName("Process unhandled event types - Should ignore and return 200")
        void handleDefaultEvent() throws Exception {
            Event mockEvent = mock(Event.class);
            EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
            when(mockEvent.getType()).thenReturn("customer.created"); // Not handled in switch
            when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
            when(mockDeserializer.getObject()).thenReturn(Optional.of(mock(com.stripe.model.Customer.class)));

            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            mockMvc.perform(post("/api/v1/payments/webhook")
                            .header("Stripe-Signature", "valid")
                            .content("payload"))
                    .andExpect(status().isOk());
        }
    }
}