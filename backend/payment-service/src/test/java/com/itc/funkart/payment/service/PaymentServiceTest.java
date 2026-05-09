package com.itc.funkart.payment.service;

import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.dto.request.CreatePaymentIntentRequest;
import com.itc.funkart.payment.entity.Payment;
import com.itc.funkart.payment.exception.PaymentException;
import com.itc.funkart.payment.repository.PaymentRepository;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>PaymentServiceTest</h2>
 *
 * <p>Comprehensive coverage for {@link PaymentService} — fixed and extended from the
 * original version which had a broken 5-arg Stripe mock (now 6 args) and several
 * missing edge-case tests:</p>
 * <ul>
 *   <li>Intent creation: success (fixed mock), DB failure, idempotency guard</li>
 *   <li>Confirmation: success, unauthorized, already-PROCESSING guard, final-state guards</li>
 *   <li>Webhooks: success idempotency, failure idempotency, refund, refund idempotency,
 *       missing object, Kafka failure</li>
 *   <li>Refund: success, already-refunded idempotency, unauthorized, invalid status, not found</li>
 *   <li>getPayment: success, unauthorized, not found</li>
 *   <li>getLatestPaymentIntent: no payments, payment exists but no Stripe intent yet</li>
 * </ul>
 *
 * <p><b>Note on {@code createIntent_Idempotent}:</b> The idempotency branch calls
 * {@code PaymentIntent.retrieve()} — a static Stripe SDK call requiring real credentials.
 * Without them it throws and gets wrapped into {@link PaymentException}. The test still
 * validates the key invariant: when a local record already has a
 * {@code stripePaymentIntentId}, {@link StripeService#createPaymentIntent} is
 * <em>never</em> invoked (no double-charge).</p>
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

    @BeforeEach
    void setUp() {
        testUser = JwtUserDto.builder()
                .id(123L).email("test@funkart.com").name("Test User").build();
    }

    private Event mockStripeEvent(String type, StripeObject obj) {
        Event event = mock(Event.class, RETURNS_DEEP_STUBS);
        when(event.getType()).thenReturn(type);
        when(event.getDataObjectDeserializer().getObject()).thenReturn(Optional.of(obj));
        return event;
    }

    // =========================================================
    // createPaymentIntent
    // =========================================================

    @Nested
    @DisplayName("createPaymentIntent")
    class CreationTests {

        @Test
        @DisplayName("Success: saves payment, calls Stripe (6-arg), returns response")
        void success() throws Exception {
            var request = new CreatePaymentIntentRequest(101L, 5000L, "usd");
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setId(1L);

            var mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_123");
            when(mockIntent.getClientSecret()).thenReturn("secret_123");

            when(mockPaymentRepository.findByOrderId(101L)).thenReturn(Optional.empty());
            when(mockPaymentRepository.save(any(Payment.class))).thenReturn(payment);
            // Correct 6-arg signature: (amount, currency, userId, paymentId, orderId, idempotencyKey)
            when(mockStripeService.createPaymentIntent(
                    eq(5000L), eq("usd"), eq(123L), eq(1L), eq(101L), anyString()))
                    .thenReturn(mockIntent);

            var response = paymentService.createPaymentIntent(testUser, request);

            assertNotNull(response);
            assertEquals("pi_123", response.paymentIntentId());
            verify(mockPaymentRepository).findByOrderId(101L);
            verify(mockPaymentRepository, atLeastOnce()).save(any());
        }

        @Test
        @DisplayName("Idempotent: existing stripePaymentIntentId → no second Stripe call")
        void idempotent_noDoubleCharge() throws StripeException {
            var request = new CreatePaymentIntentRequest(101L, 5000L, "usd");
            var existing = new Payment(testUser.id(), 101L, 5000L, "usd");
            existing.setId(1L);
            existing.setStripePaymentIntentId("pi_existing");

            when(mockPaymentRepository.findByOrderId(101L)).thenReturn(Optional.of(existing));

            // Static PaymentIntent.retrieve() fails without real API key → wrapped in PaymentException.
            // Key invariant: createPaymentIntent is NEVER called (no double-charge).
            assertThrows(PaymentException.class,
                    () -> paymentService.createPaymentIntent(testUser, request));

            verify(mockStripeService, never()).createPaymentIntent(
                    anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("Fault: DB exception wrapped into PaymentException")
        void dbFailure() {
            when(mockPaymentRepository.findByOrderId(anyLong()))
                    .thenThrow(new DataAccessException("DB Down") {
                    });

            assertThrows(PaymentException.class,
                    () -> paymentService.createPaymentIntent(testUser,
                            new CreatePaymentIntentRequest(101L, 5000L, "usd")));
        }
    }

    // =========================================================
    // confirmPayment
    // =========================================================

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmationTests {

        @Test
        @DisplayName("Success: transitions to PROCESSING")
        void success() throws Exception {
            var request = new ConfirmPaymentRequest("pi_123", "pm_card", "http://return.url");
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123"))
                    .thenReturn(Optional.of(payment));

            var response = paymentService.confirmPayment(testUser, request);

            assertEquals("PROCESSING", payment.getStatus());
            assertEquals("PROCESSING", response.status());
            verify(mockStripeService).confirmPaymentIntent(
                    eq("pi_123"), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Security: unauthorized user throws PaymentException")
        void unauthorized() {
            var intruder = JwtUserDto.builder().id(999L).build();
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123"))
                    .thenReturn(Optional.of(payment));

            assertThrows(PaymentException.class, () ->
                    paymentService.confirmPayment(intruder,
                            new ConfirmPaymentRequest("pi_123", "pm", "url")));
            verifyNoInteractions(mockStripeService);
        }

        @Test
        @DisplayName("Guard: PROCESSING status returns early without Stripe call")
        void alreadyProcessing() {
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus("PROCESSING");
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123"))
                    .thenReturn(Optional.of(payment));

            var response = paymentService.confirmPayment(testUser,
                    new ConfirmPaymentRequest("pi_123", "pm", "url"));

            assertEquals("PROCESSING", response.status());
            verifyNoInteractions(mockStripeService);
        }

        @ParameterizedTest(name = "Final status \"{0}\" returns early")
        @ValueSource(strings = {"succeeded", "failed", "refunded"})
        @DisplayName("Guard: final states return early without Stripe call")
        void finalStatesReturnEarly(String finalStatus) {
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus(finalStatus);
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123"))
                    .thenReturn(Optional.of(payment));

            var response = paymentService.confirmPayment(testUser,
                    new ConfirmPaymentRequest("pi_123", "pm", "url"));

            assertEquals(finalStatus, response.status());
            verifyNoInteractions(mockStripeService);
        }
    }

    // =========================================================
    // processWebhookEvent
    // =========================================================

    @Nested
    @DisplayName("processWebhookEvent")
    class WebhookTests {

        @Test
        @DisplayName("Idempotency: already-succeeded ignores success event")
        void handleSuccess_idempotent() {
            var intent = mock(PaymentIntent.class);
            when(intent.getId()).thenReturn("pi_123");
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus("succeeded");
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123"))
                    .thenReturn(Optional.of(payment));

            paymentService.processWebhookEvent(mockStripeEvent("payment_intent.succeeded", intent));

            verify(mockKafkaEventPublisher, never()).publishPaymentCompletedEvent(any());
            verify(mockPaymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Failure webhook: sets 'failed' and publishes event")
        void handlePaymentFailure() {
            PaymentIntent intent = mock(PaymentIntent.class, RETURNS_DEEP_STUBS);
            when(intent.getId()).thenReturn("pi_fail");
            lenient().when(intent.getLastPaymentError().getMessage()).thenReturn("Card Declined");
            lenient().when(intent.getLastPaymentError().getCode()).thenReturn("card_declined");
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            lenient().when(mockPaymentRepository.findByStripePaymentIntentId("pi_fail"))
                    .thenReturn(Optional.of(payment));

            paymentService.processWebhookEvent(mockStripeEvent("payment_intent.payment_failed", intent));

            assertEquals("failed", payment.getStatus());
            verify(mockKafkaEventPublisher).publishPaymentFailedEvent(any());
        }

        @Test
        @DisplayName("Failure idempotency: already-failed ignores second failure event")
        void handlePaymentFailure_idempotent() {
            PaymentIntent intent = mock(PaymentIntent.class, RETURNS_DEEP_STUBS);
            when(intent.getId()).thenReturn("pi_fail");
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus("failed");
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_fail"))
                    .thenReturn(Optional.of(payment));

            paymentService.processWebhookEvent(mockStripeEvent("payment_intent.payment_failed", intent));

            verify(mockKafkaEventPublisher, never()).publishPaymentFailedEvent(any());
            verify(mockPaymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Refund webhook: sets 'refunded' and publishes event")
        void handleRefund() {
            Charge charge = mock(Charge.class);
            RefundCollection refunds = mock(RefundCollection.class);
            Refund refund = mock(Refund.class);
            when(charge.getPaymentIntent()).thenReturn("pi_123");
            when(charge.getRefunds()).thenReturn(refunds);
            when(refunds.getData()).thenReturn(Collections.singletonList(refund));
            when(charge.getAmountRefunded()).thenReturn(2000L);
            when(refund.getId()).thenReturn("re_123");
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123"))
                    .thenReturn(Optional.of(payment));

            paymentService.processWebhookEvent(mockStripeEvent("charge.refunded", charge));

            assertEquals("refunded", payment.getStatus());
            verify(mockKafkaEventPublisher).publishPaymentRefundedEvent(any());
        }

        @Test
        @DisplayName("Refund idempotency: already-refunded ignores second refund event")
        void handleRefund_idempotent() {
            Charge charge = mock(Charge.class);
            when(charge.getPaymentIntent()).thenReturn("pi_123");
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus("refunded");
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123"))
                    .thenReturn(Optional.of(payment));

            paymentService.processWebhookEvent(mockStripeEvent("charge.refunded", charge));

            verify(mockKafkaEventPublisher, never()).publishPaymentRefundedEvent(any());
            verify(mockPaymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Fault: missing Stripe object throws PaymentException")
        void missingObject() throws EventDataObjectDeserializationException {
            Event event = mock(Event.class, RETURNS_DEEP_STUBS);
            when(event.getType()).thenReturn("payment_intent.succeeded");
            when(event.getDataObjectDeserializer().getObject()).thenReturn(Optional.empty());
            when(event.getDataObjectDeserializer().deserializeUnsafe())
                    .thenThrow(new RuntimeException("cannot deserialize"));

            assertThrows(PaymentException.class, () -> paymentService.processWebhookEvent(event));
        }

        @Test
        @DisplayName("Resiliency: Kafka failure propagates to trigger rollback")
        void kafkaFailure() {
            PaymentIntent intent = mock(PaymentIntent.class);
            when(intent.getId()).thenReturn("pi_123");
            when(intent.getAmountReceived()).thenReturn(5000L);
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123"))
                    .thenReturn(Optional.of(payment));
            doThrow(new RuntimeException("Kafka Down"))
                    .when(mockKafkaEventPublisher).publishPaymentCompletedEvent(any());

            assertThrows(RuntimeException.class, () ->
                    paymentService.processWebhookEvent(
                            mockStripeEvent("payment_intent.succeeded", intent)));
        }
    }

    // =========================================================
    // refundPayment
    // =========================================================

    @Nested
    @DisplayName("refundPayment")
    class RefundTests {

        @Test
        @DisplayName("Success: refunds succeeded payment and publishes event")
        void success() throws Exception {
            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setId(1L);
            payment.setStatus("succeeded");
            payment.setStripePaymentIntentId("pi_123");
            Refund mockRefund = mock(Refund.class);
            when(mockRefund.getId()).thenReturn("re_abc");
            when(mockRefund.getAmount()).thenReturn(5000L);
            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            when(mockStripeService.refundPayment("pi_123", 1L)).thenReturn(mockRefund);

            var response = paymentService.refundPayment(testUser, 1L);

            assertEquals("refunded", response.status());
            verify(mockKafkaEventPublisher).publishPaymentRefundedEvent(any());
        }

        @Test
        @DisplayName("Idempotent: already-refunded returns existing without Stripe call")
        void alreadyRefunded() {
            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setId(1L);
            payment.setStatus("refunded");
            payment.setStripePaymentIntentId("pi_123");
            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            var response = paymentService.refundPayment(testUser, 1L);

            assertEquals("refunded", response.status());
            verifyNoInteractions(mockStripeService);
            verifyNoInteractions(mockKafkaEventPublisher);
        }

        @Test
        @DisplayName("Security: unauthorized user throws PaymentException")
        void unauthorized() {
            var intruder = JwtUserDto.builder().id(999L).build();
            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setId(1L);
            payment.setStatus("succeeded");
            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThrows(PaymentException.class, () -> paymentService.refundPayment(intruder, 1L));
            verifyNoInteractions(mockStripeService);
        }

        @ParameterizedTest(name = "Status \"{0}\" is not refundable")
        @ValueSource(strings = {"PROCESSING", "failed", "PENDING"})
        @DisplayName("Constraint: non-succeeded statuses throw PaymentException")
        void invalidState(String status) {
            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setId(1L);
            payment.setStatus(status);
            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThrows(PaymentException.class, () -> paymentService.refundPayment(testUser, 1L));
            verifyNoInteractions(mockStripeService);
        }

        @Test
        @DisplayName("NotFound: unknown paymentId throws PaymentException")
        void notFound() {
            when(mockPaymentRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(PaymentException.class, () -> paymentService.refundPayment(testUser, 999L));
        }
    }

    // =========================================================
    // getPayment
    // =========================================================

    @Nested
    @DisplayName("getPayment")
    class GetPaymentTests {

        @Test
        @DisplayName("Success: authorized user retrieves payment")
        void success() {
            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setId(1L);
            payment.setStatus("succeeded");
            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertEquals("succeeded", paymentService.getPayment(testUser, 1L).status());
        }

        @Test
        @DisplayName("Security: unauthorized user throws PaymentException")
        void unauthorized() {
            var intruder = JwtUserDto.builder().id(404L).build();
            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setId(1L);
            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThrows(PaymentException.class, () -> paymentService.getPayment(intruder, 1L));
        }

        @Test
        @DisplayName("NotFound: unknown paymentId throws PaymentException")
        void notFound() {
            when(mockPaymentRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(PaymentException.class, () -> paymentService.getPayment(testUser, 999L));
        }
    }

    // =========================================================
    // getLatestPaymentIntent
    // =========================================================

    @Nested
    @DisplayName("getLatestPaymentIntent")
    class LatestIntentTests {

        @Test
        @DisplayName("Empty: no payments for user → empty Optional")
        void noPayments() {
            when(mockPaymentRepository.findTopByUserIdOrderByCreatedAtDesc(testUser.id()))
                    .thenReturn(Optional.empty());

            assertTrue(paymentService.getLatestPaymentIntent(testUser).isEmpty());
        }

        @Test
        @DisplayName("Pending: payment exists but Stripe intent not yet created → empty Optional")
        void noStripeIntent() {
            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setId(1L);
            // stripePaymentIntentId is null
            when(mockPaymentRepository.findTopByUserIdOrderByCreatedAtDesc(testUser.id()))
                    .thenReturn(Optional.of(payment));

            assertTrue(paymentService.getLatestPaymentIntent(testUser).isEmpty());
        }
    }
}
