package com.itc.funkart.payment.service;

import com.itc.funkart.common.dto.user.JwtUserDto;
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
 * Comprehensive coverage for PaymentService business logic, updated for 2026
 * with unified DTO structures and builder-based event publishing.
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
                .id(123L)
                .email("test@funkart.com")
                .name("Test User")
                .build();
    }

    private Event mockStripeEvent(String type, StripeObject obj) {
        Event event = mock(Event.class, RETURNS_DEEP_STUBS);
        when(event.getType()).thenReturn(type);
        when(event.getDataObjectDeserializer().getObject()).thenReturn(Optional.of(obj));
        return event;
    }

    @Nested
    @DisplayName("Creation Flow: Intent & Persistence")
    class CreationTests {

        @Test
        @DisplayName("Success: Should use orElseGet logic and return PaymentIntentResponse")
        void createIntent_Success() throws Exception {
            var request = new CreatePaymentIntentRequest(101L, 5000L, "usd");
            var payment = new Payment(testUser.id(), request.orderId(), request.amount(), request.currency());
            payment.setId(1L);

            var mockStripeIntent = mock(PaymentIntent.class);
            when(mockStripeIntent.getId()).thenReturn("pi_123");
            when(mockStripeIntent.getClientSecret()).thenReturn("secret_123");

            // Mock repository to handle the find-or-create logic
            when(mockPaymentRepository.findByOrderId(101L)).thenReturn(Optional.empty());
            when(mockPaymentRepository.save(any(Payment.class))).thenReturn(payment);

            when(mockStripeService.createPaymentIntent(eq(5000L), eq("usd"), eq(123L), eq(1L), eq(100L)))
                    .thenReturn(mockStripeIntent);

            var response = paymentService.createPaymentIntent(testUser, request);

            assertNotNull(response);
            assertEquals("pi_123", response.paymentIntentId());
            // Verify findByOrderId was called as per the new service logic
            verify(mockPaymentRepository).findByOrderId(101L);
            verify(mockPaymentRepository, atLeastOnce()).save(any());
        }

        @Test
        @DisplayName("Fault: Should wrap DB exceptions into PaymentException")
        void createIntent_DbFailure() {
            var request = new CreatePaymentIntentRequest(101L, 5000L, "usd");
            when(mockPaymentRepository.findByOrderId(anyLong())).thenThrow(new DataAccessException("DB Down") {
            });

            assertThrows(PaymentException.class, () -> paymentService.createPaymentIntent(testUser, request));
        }
    }

    @Nested
    @DisplayName("Transaction State & Security")
    class ConfirmationTests {

        @Test
        @DisplayName("Success: Should update status to PROCESSING")
        void confirm_Success() throws Exception {
            var request = new ConfirmPaymentRequest("pi_123", "pm_card", "url");
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");

            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123")).thenReturn(Optional.of(payment));

            var response = paymentService.confirmPayment(testUser, request);

            assertEquals("PROCESSING", payment.getStatus());
            assertEquals("PROCESSING", response.status());
            verify(mockStripeService).confirmPaymentIntent(eq("pi_123"), anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("Security: Block unauthorized confirmation")
        void confirm_Unauthorized() {
            var intruder = JwtUserDto.builder().id(999L).build();
            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");

            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123")).thenReturn(Optional.of(payment));

            assertThrows(PaymentException.class, () ->
                    paymentService.confirmPayment(intruder, new ConfirmPaymentRequest("pi_123", "pm", "url")));
        }
    }

    @Nested
    @DisplayName("Webhook Reconciliation")
    class WebhookTests {

        @Test
        @DisplayName("Idempotency: Ignore success events for terminal states")
        void handleSuccess_Idempotent() {
            var intent = mock(PaymentIntent.class);
            when(intent.getId()).thenReturn("pi_123");

            var payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus("succeeded");

            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123")).thenReturn(Optional.of(payment));

            paymentService.processWebhookEvent(mockStripeEvent("payment_intent.succeeded", intent));

            verify(mockKafkaEventPublisher, never()).publishPaymentCompletedEvent(any());
        }

        @Test
        @DisplayName("Refund Reconcile: Update local status from Charge webhook")
        void handleRefund_Success() {
            Charge charge = mock(Charge.class);
            RefundCollection refundCollection = mock(RefundCollection.class);
            Refund refund = mock(Refund.class);

            when(charge.getPaymentIntent()).thenReturn("pi_123");
            when(charge.getRefunds()).thenReturn(refundCollection);
            when(refundCollection.getData()).thenReturn(Collections.singletonList(refund));
            when(charge.getAmountRefunded()).thenReturn(2000L);
            when(refund.getId()).thenReturn("re_123");

            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123")).thenReturn(Optional.of(payment));

            paymentService.processWebhookEvent(mockStripeEvent("charge.refunded", charge));

            assertEquals("refunded", payment.getStatus());
            verify(mockKafkaEventPublisher).publishPaymentRefundedEvent(any());
        }
    }

    @Nested
    @DisplayName("Refund Logic & Constraints")
    class RefundTests {

        @Test
        @DisplayName("Success: Refund succeeded payment and publish event")
        void refund_Success() throws Exception {
            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus("succeeded");
            payment.setStripePaymentIntentId("pi_123");

            Refund mockRefund = mock(Refund.class);
            when(mockRefund.getId()).thenReturn("re_abc");
            when(mockRefund.getAmount()).thenReturn(5000L);

            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            when(mockStripeService.refundPayment("pi_123", 1L)).thenReturn(mockRefund);

            var response = paymentService.refundPayment(testUser, 1L);

            assertEquals("refunded", payment.getStatus());
            assertEquals("refunded", response.status());
            verify(mockKafkaEventPublisher).publishPaymentRefundedEvent(any());
        }

        @Test
        @DisplayName("Constraint: Block refund for non-succeeded status")
        void refund_InvalidState() {
            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            payment.setStatus("PROCESSING");

            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThrows(PaymentException.class, () -> paymentService.refundPayment(testUser, 1L));
            verifyNoInteractions(mockStripeService);
        }
    }

    @Nested
    @DisplayName("Resiliency & Edge Cases")
    class ResilienceTests {

        @Test
        @DisplayName("Kafka Error: Exception should bubble up to trigger rollback")
        void kafkaFailure() {
            PaymentIntent intent = mock(PaymentIntent.class);
            when(intent.getId()).thenReturn("pi_123");
            when(intent.getAmountReceived()).thenReturn(5000L);

            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123")).thenReturn(Optional.of(payment));

            doThrow(new RuntimeException("Kafka Down")).when(mockKafkaEventPublisher).publishPaymentCompletedEvent(any());

            assertThrows(RuntimeException.class, () ->
                    paymentService.processWebhookEvent(mockStripeEvent("payment_intent.succeeded", intent)));
        }

//        @Test
//        @DisplayName("Lookup: Verify ownership check in getPayment")
//        void getPayment_Unauthorized() {
//            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
//            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));
//
//            JwtUserDto intruder = JwtUserDto.builder().id(404L).build();
//
//            assertThrows(PaymentException.class, () -> paymentService.confirmPayment(intruder, 1L));
//        }

        @Test
        @DisplayName("Webhook: Throw PaymentException on missing object")
        void handleWebhook_MissingObject() {
            Event event = mock(Event.class, RETURNS_DEEP_STUBS);
            when(event.getType()).thenReturn("payment_intent.succeeded");
            when(event.getDataObjectDeserializer().getObject()).thenReturn(Optional.empty());

            assertThrows(PaymentException.class, () -> paymentService.processWebhookEvent(event));
        }

        @Test
        @DisplayName("Webhook Failure: Handle payment_intent.payment_failed")
        void handlePaymentFailure_Success() {
            // 1. Create the intent with deep stubs so nested calls don't return null
            PaymentIntent intent = mock(PaymentIntent.class, RETURNS_DEEP_STUBS);

            // 2. Setup the "chain" of data in one go
            when(intent.getId()).thenReturn("pi_fail");
            when(intent.getLastPaymentError().getMessage()).thenReturn("Card Declined");
            when(intent.getLastPaymentError().getCode()).thenReturn("card_declined");

            // 3. Setup the local record
            Payment payment = new Payment(testUser.id(), 101L, 5000L, "usd");
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_fail"))
                    .thenReturn(Optional.of(payment));

            // 4. Run the process
            paymentService.processWebhookEvent(mockStripeEvent("payment_intent.payment_failed", intent));

            // 5. Verify the results
            assertEquals("failed", payment.getStatus());
            verify(mockKafkaEventPublisher).publishPaymentFailedEvent(any());
        }
    }
}