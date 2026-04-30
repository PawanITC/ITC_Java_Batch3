package com.itc.funkart.payment.service;

import com.itc.funkart.payment.dto.jwt.JwtUserDto;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.dto.request.CreatePaymentIntentRequest;
import com.itc.funkart.payment.entity.Payment;
import com.itc.funkart.payment.exception.PaymentException;
import com.itc.funkart.payment.repository.PaymentRepository;
import com.stripe.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>PaymentServiceTest</h2>
 * <p>
 * This suite provides comprehensive coverage for the business logic within {@link PaymentService}.
 * It utilizes Mockito to isolate the service from external infrastructure (Stripe API, Database, Kafka).
 * </p>
 *
 * <h3>Testing Strategy:</h3>
 * <ul>
 * <li><b>Isolation:</b> All external collaborators (Repository, StripeService, KafkaPublisher) are mocked.</li>
 * <li><b>Security-First:</b> Explicit tests validate that users cannot interact with payments they do not own.</li>
 * <li><b>Integrity:</b> Validates state transitions (PENDING → PROCESSING → SUCCEEDED) and idempotency.</li>
 * <li><b>Stripe Integration:</b> Uses "Deep Stubbing" to simulate the complex, often nested, Stripe SDK models.</li>
 * </ul>
 *
 * @author Abbas
 * @version 1.4
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository mockPaymentRepository;

    @Mock
    private StripeService mockStripeService;

    @Mock
    private KafkaEventPublisher mockKafkaEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private JwtUserDto testUser;

    /**
     * Re-initializes a standard mock user before every test to ensure isolation.
     */
    @BeforeEach
    void setUp() {
        testUser = JwtUserDto.builder()
                .id(123L)
                .email("test@funkart.com")
                .name("Test User")
                .role("ROLE_USER")
                .build();
    }

    /**
     * <b>Technical Workaround:</b> Bypasses Stripe's complex internal deserialization logic.
     * <p>
     * Stripe's {@link Event} uses internal 'EventData' classes with restricted visibility.
     * Using {@code RETURNS_DEEP_STUBS} allows the test to mock the chain
     * {@code event.getDataObjectDeserializer().getObject()} without triggering ClassNotFound errors.
     * </p>
     * * @param type The Stripe event string (e.g., "payment_intent.succeeded").
     *
     * @param obj The model object (Intent, Charge, etc.) to inject into the event.
     * @return A deep-stubbed Event mock.
     */
    private Event mockStripeEvent(String type, StripeObject obj) {
        Event event = mock(Event.class, RETURNS_DEEP_STUBS);
        when(event.getType()).thenReturn(type);
        when(event.getDataObjectDeserializer().getObject()).thenReturn(Optional.of(obj));
        return event;
    }

    /**
     * <h3>Payment Initiation</h3>
     * Validates the first step of the payment lifecycle where local records meet Stripe intents.
     */
    @Nested
    @DisplayName("Creation Flow: Stripe Intent & Persistence")
    class CreationTests {

        /**
         * Verifies that the service creates a local 'PENDING' record and then links it to a Stripe Intent.
         */
        @Test
        @DisplayName("Success: Should persist local record and create Stripe intent")
        void createIntent_Success() throws Exception {
            var request = new CreatePaymentIntentRequest(101L, 5000L, "usd");
            var savedPayment = new Payment(testUser.id(), request.orderId(), request.amount(), request.currency());
            savedPayment.setId(1L); // Simulating DB auto-increment for Stripe metadata

            var mockStripeIntent = mock(PaymentIntent.class);
            when(mockStripeIntent.getId()).thenReturn("pi_123");

            when(mockPaymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
            when(mockStripeService.createPaymentIntent(eq(5000L), eq("usd"), eq(123L), eq(1L)))
                    .thenReturn(mockStripeIntent);

            var response = paymentService.createPaymentIntent(testUser, request);

            assertNotNull(response);
            assertEquals("pi_123", response.getData().paymentIntentId());
            verify(mockPaymentRepository, times(2)).save(any());
        }

        /**
         * Ensures that low-level DB errors are caught and re-thrown as domain-specific {@link PaymentException}.
         */
        @Test
        @DisplayName("Fault: Should wrap DataAccessException into domain PaymentException")
        void createIntent_DbFailure() {
            var request = new CreatePaymentIntentRequest(101L, 5000L, "usd");
            when(mockPaymentRepository.save(any())).thenThrow(new DataAccessException("Connection Refused") {
            });

            assertThrows(PaymentException.class, () -> paymentService.createPaymentIntent(testUser, request));
        }
    }

    /**
     * <h3>State Management & Security</h3>
     * Focuses on the "Processing" state and owner authorization.
     */
    @Nested
    @DisplayName("Transaction State: Authorization & Confirmation")
    class ConfirmationTests {

        /**
         * Validates the transition of the payment status to 'PROCESSING' upon successful confirmation.
         */
        @Test
        @DisplayName("Success: Should move Payment to PROCESSING state")
        void confirm_Success() throws Exception {
            var request = new ConfirmPaymentRequest("pi_123", "pm_card", "http://funkart.com/callback");
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");

            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123")).thenReturn(Optional.of(payment));

            paymentService.confirmPayment(testUser, request);

            assertEquals("PROCESSING", payment.getStatus());
            verify(mockStripeService).confirmPaymentIntent(eq("pi_123"), eq("pm_card"), eq("http://funkart.com/callback"));
            verify(mockPaymentRepository).save(payment);
        }

        /**
         * <b>Logic Firewall:</b> Ensures that User A cannot confirm a payment intent created by User B.
         */
        @Test
        @DisplayName("Security: Should block users attempting to confirm payments they don't own")
        void confirm_Unauthorized() {
            var intruder = JwtUserDto.builder().id(999L).build();
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");

            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123")).thenReturn(Optional.of(payment));

            assertThrows(PaymentException.class, () ->
                    paymentService.confirmPayment(intruder, new ConfirmPaymentRequest("pi_123", "pm", "url")));
        }
    }

    /**
     * <h3>Webhook Reconciliation</h3>
     * Tests the handling of asynchronous notifications from Stripe.
     */
    @Nested
    @DisplayName("Webhook Handling: Stripe Event Dispatcher")
    class WebhookTests {

        /**
         * <b>Idempotency Check:</b> Verifies that the service ignores success events
         * for payments that are already marked as 'succeeded' in the DB.
         */
        @Test
        @DisplayName("Idempotency: Should ignore 'succeeded' events if state is already terminal")
        void handleSuccess_Idempotent() {
            var intent = mock(PaymentIntent.class);
            when(intent.getId()).thenReturn("pi_123");

            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus("succeeded");

            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123")).thenReturn(Optional.of(payment));

            paymentService.processWebhookEvent(mockStripeEvent("payment_intent.succeeded", intent));

            verify(mockKafkaEventPublisher, never()).publishPaymentCompletedEvent(any());
        }

        /**
         * Validates the parsing logic for Stripe Charge/Refund objects to update local records.
         */
        @Test
        @DisplayName("Refund Reconcile: Should extract refund amount from Stripe Charge object")
        void handleRefund_Success() {
            Charge charge = mock(Charge.class);
            RefundCollection refundCollection = mock(RefundCollection.class);
            Refund refund = mock(Refund.class);

            when(charge.getPaymentIntent()).thenReturn("pi_123");
            when(charge.getRefunds()).thenReturn(refundCollection);
            when(refundCollection.getData()).thenReturn(Collections.singletonList(refund));
            when(refund.getAmount()).thenReturn(2000L);

            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus("succeeded");

            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123")).thenReturn(Optional.of(payment));

            paymentService.processWebhookEvent(mockStripeEvent("charge.refunded", charge));

            assertEquals("refunded", payment.getStatus());
            verify(mockKafkaEventPublisher).publishPaymentRefundedEvent(any());
        }
    }

    /**
     * <h3>Resiliency & Infrastructure Faults</h3>
     * Tests how the system behaves when external messaging components fail.
     */
    @Nested
    @DisplayName("Fault Tolerance: Distributed System Resiliency")
    class EdgeCaseTests {

        /**
         * Verifies that a Kafka publishing failure bubbles up to the controller,
         * effectively triggering a DB transaction rollback.
         */
        @Test
        @DisplayName("Kafka Error: Should bubble up exception to trigger Transaction Rollback")
        void kafkaFailure() {
            PaymentIntent intent = mock(PaymentIntent.class);
            when(intent.getId()).thenReturn("pi_123");

            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus("PROCESSING");

            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123"))
                    .thenReturn(Optional.of(payment));

            // Use lenient() to allow the service to run despite strict Mockito rules
            lenient().doThrow(new RuntimeException("Kafka Broker Unavailable"))
                    .when(mockKafkaEventPublisher).publishPaymentCompletedEvent(any());

            Event stripeEvent = mockStripeEvent("payment_intent.succeeded", intent);

            assertThrows(RuntimeException.class, () ->
                    paymentService.processWebhookEvent(stripeEvent));
        }
    }

    /**
     * <h3>Refund Logic & Constraints</h3>
     * Validates that refunds only happen for successful payments and handle Stripe errors.
     */
    @Nested
    @DisplayName("Refund Flow: Business Rules & Stripe Integration")
    class RefundTests {

        @Test
        @DisplayName("Success: Should process refund for 'succeeded' payment")
        void refund_Success() throws Exception {
            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus("succeeded");
            payment.setStripePaymentIntentId("pi_123");

            Refund mockRefund = mock(Refund.class);

            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            when(mockStripeService.refundPayment("pi_123")).thenReturn(mockRefund);

            var response = paymentService.refundPayment(testUser, 1L);

            assertEquals("refunded", payment.getStatus());
            assertEquals("refunded", response.getData().status());
            verify(mockKafkaEventPublisher).publishPaymentRefundedEvent(any());
        }

        @Test
        @DisplayName("Constraint: Should block refund if payment hasn't succeeded")
        void refund_InvalidState() {
            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus("PROCESSING"); // Not succeeded!

            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            PaymentException ex = assertThrows(PaymentException.class,
                    () -> paymentService.refundPayment(testUser, 1L));

            assertTrue(ex.getMessage().contains("Can only refund payments that have succeeded"));
            verifyNoInteractions(mockStripeService);
        }

        @Test
        @DisplayName("Security: Should block refund if user doesn't own the payment")
        void refund_Unauthorized() {
            JwtUserDto intruder = JwtUserDto.builder().id(999L).build();
            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");

            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThrows(PaymentException.class, () -> paymentService.refundPayment(intruder, 1L));
        }
    }

    /**
     * <h3>Extended Webhook Scenarios</h3>
     * Covers the specific failure and retrieval branches in the webhook handler.
     */
    @Nested
    @DisplayName("Webhook Edge Cases: Failures & Retrieval")
    class WebhookExtendedTests {

        @Test
        @DisplayName("Failure: Should update status to 'failed' on payment_intent.payment_failed")
        void handlePaymentFailure_Success() {
            PaymentIntent intent = mock(PaymentIntent.class);
            when(intent.getId()).thenReturn("pi_fail");

            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus("PROCESSING");

            when(mockPaymentRepository.findByStripePaymentIntentId("pi_fail")).thenReturn(Optional.of(payment));

            paymentService.processWebhookEvent(mockStripeEvent("payment_intent.payment_failed", intent));

            assertEquals("failed", payment.getStatus());
            verify(mockKafkaEventPublisher).publishPaymentFailedEvent(any());
        }

        @Test
        @DisplayName("Fault: Should throw Exception if event data is missing")
        void handleWebhook_MissingObject() {
            // 1. Setup mock with deep stubs
            Event event = mock(Event.class, RETURNS_DEEP_STUBS);

            // 2. Provide a type so the switch statement doesn't throw NPE
            when(event.getType()).thenReturn("payment_intent.succeeded");

            // 3. Simulate the scenario where Stripe data is missing/corrupt
            when(event.getDataObjectDeserializer().getObject()).thenReturn(Optional.empty());
            // Also ensure the deprecated fallback returns null for the .or() block
            when(event.getData().getObject()).thenReturn(null);

            // 4. Assert that your custom PaymentException is thrown, not a NullPointerException
            assertThrows(PaymentException.class, () -> paymentService.processWebhookEvent(event));
        }

        @Test
        @DisplayName("Coverage: Should ignore unknown event types")
        void handleWebhook_UnknownEvent() {
            Event event = mockStripeEvent("customer.created", mock(Customer.class));

            // This hits the 'default' branch in your switch statement
            assertDoesNotThrow(() -> paymentService.processWebhookEvent(event));
            verifyNoInteractions(mockKafkaEventPublisher);
        }
    }
}