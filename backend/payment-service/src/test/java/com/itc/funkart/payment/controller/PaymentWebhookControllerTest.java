package com.itc.funkart.payment.controller;

import com.itc.funkart.payment.config.ApiConfig;
import com.itc.funkart.payment.exception.PaymentException;
import com.itc.funkart.payment.service.JwtService;
import com.itc.funkart.payment.service.PaymentService;
import com.stripe.model.*;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>PaymentWebhookControllerTest</h2>
 * <p>
 * This test suite performs a <b>Web Slice</b> test of the {@link PaymentWebhookController}.
 * It validates the integration with Stripe's asynchronous notification system.
 * </p>
 * * <h3>Key Technical Strategies:</h3>
 * <ul>
 * <li><b>Static Mocking:</b> Uses {@code MockedStatic<Webhook>} to intercept the Stripe SDK's
 * signature verification logic without requiring real API keys.</li>
 * <li><b>Deep Stubbing:</b> Utilizes {@code RETURNS_DEEP_STUBS} to simulate the complex,
 * nested deserialization hierarchy of Stripe's {@link Event} objects.</li>
 * <li><b>Security Validation:</b> Tests the boundary between authorized Stripe requests
 * and potentially malicious payloads.</li>
 * </ul>
 *
 * @author Abbas
 * @version 1.3
 */
@WebMvcTest(PaymentWebhookController.class)
@AutoConfigureMockMvc(addFilters = false) // Webhooks are secured by Stripe signatures, not JWT filters
class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private ApiConfig apiConfig;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private RestClient restClient;

    /**
     * Static mock context for Stripe Webhook utility.
     * Must be manually closed to avoid thread-local leakage.
     */
    private MockedStatic<Webhook> mockedWebhook;

    /**
     * Initializes the static mock for {@link Webhook} and stubs common infrastructure.
     */
    @BeforeEach
    void setUp() {
        mockedWebhook = mockStatic(Webhook.class);
        when(apiConfig.getVersion()).thenReturn("api/v1");
    }

    /**
     * Cleans up static mock resources after each test execution.
     */
    @AfterEach
    void tearDown() {
        if (mockedWebhook != null) {
            mockedWebhook.close();
        }
    }

    /**
     * <b>Universal Mock Helper:</b>
     * Bypasses the package-private visibility of Stripe internal classes (like EventData).
     * By using {@code RETURNS_DEEP_STUBS}, we can mock the chain:
     * {@code event.getDataObjectDeserializer().getObject()} in a single setup.
     * * @param type The Stripe Event type (e.g., "payment_intent.succeeded").
     *
     * @param obj The specific Stripe model object to be returned in the data payload.
     * @return A deep-stubbed Event mock.
     */
    private Event mockStripeEvent(String type, StripeObject obj) {
        Event event = mock(Event.class, RETURNS_DEEP_STUBS);
        when(event.getType()).thenReturn(type);
        when(event.getDataObjectDeserializer().getObject()).thenReturn(Optional.of(obj));
        return event;
    }

    /**
     * <h3>Success Flow Tests</h3>
     * Tests valid payloads that should be correctly routed to the service layer.
     */
    @Nested
    @DisplayName("Webhook: Positive Scenarios")
    class PositiveTests {

        /**
         * Verifies that a standard payment success event returns 200 OK
         * and triggers the centralized service processing.
         */
        @Test
        @DisplayName("POST /webhook - payment_intent.succeeded")
        void handleSucceededEvent() throws Exception {
            PaymentIntent intent = mock(PaymentIntent.class);
            Event event = mockStripeEvent("payment_intent.succeeded", intent);

            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);

            mockMvc.perform(post("/payments/webhook")
                            .header("Stripe-Signature", "valid")
                            .content("{}")
                            .contentType(MediaType.TEXT_PLAIN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Event processed: payment_intent.succeeded"));

            verify(paymentService).processWebhookEvent(any(Event.class));
        }

        /**
         * Verifies that refund events are correctly received and processed.
         */
        @Test
        @DisplayName("POST /webhook - charge.refunded")
        void handleRefundEvent() throws Exception {
            Charge charge = mock(Charge.class);
            Event event = mockStripeEvent("charge.refunded", charge);

            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);

            mockMvc.perform(post("/payments/webhook")
                            .header("Stripe-Signature", "valid")
                            .content("{}"))
                    .andExpect(status().isOk());

            verify(paymentService).processWebhookEvent(any(Event.class));
        }
    }

    /**
     * <h3>Security & Failure Flow Tests</h3>
     * Ensures the system robustly handles invalid signatures and corrupted data.
     */
    @Nested
    @DisplayName("Webhook: Negative & Security Scenarios")
    class NegativeTests {

        /**
         * <b>Scenario:</b> The request contains an invalid cryptographic signature.<br>
         * <b>Expectation:</b> Returns 401 Unauthorized to satisfy security best practices.
         */
        @Test
        @DisplayName("Invalid Signature - Should return 401")
        void handleInvalidSignature() throws Exception {
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenThrow(new com.stripe.exception.SignatureVerificationException("Fail", "sig"));

            mockMvc.perform(post("/payments/webhook")
                            .header("Stripe-Signature", "fake")
                            .content("bad_payload"))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * <b>Scenario:</b> The signature is valid, but the JSON payload is malformed
         * or the internal Stripe object is missing.<br>
         * <b>Expectation:</b> Returns 400 Bad Request via {@link PaymentException}.
         */
        @Test
        @DisplayName("Malformed Payload - Should return 400")
        void handleNullObject() throws Exception {
            Event event = mock(Event.class, RETURNS_DEEP_STUBS);

            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);

            // Stubbing the service to simulate the .orElseThrow() behavior for missing data
            doThrow(new PaymentException("Failed to extract data"))
                    .when(paymentService).processWebhookEvent(event);

            mockMvc.perform(post("/payments/webhook")
                            .header("Stripe-Signature", "valid")
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        /**
         * <b>Scenario:</b> Stripe sends an event type that the system does not handle.<br>
         * <b>Expectation:</b> Returns 200 OK but does not perform business logic.
         * (Stripe requires 2xx for ignored events to stop retries).
         */
        @Test
        @DisplayName("Unhandled Event - Should return 200 (Ignore Silently)")
        void handleUnhandledEvent() throws Exception {
            Customer customer = mock(Customer.class);
            Event event = mockStripeEvent("customer.created", customer);

            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);

            mockMvc.perform(post("/payments/webhook")
                            .header("Stripe-Signature", "valid")
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }
}