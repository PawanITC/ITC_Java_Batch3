package com.itc.funkart.payment.service;

import com.itc.funkart.payment.dto.event.PaymentCompletedEvent;
import com.itc.funkart.payment.dto.event.PaymentFailedEvent;
import com.itc.funkart.payment.dto.event.PaymentRefundedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test suite for {@link KafkaEventPublisher}.
 * <p>
 * Ensures the Payment Service acts as a reliable "Voice" for the ecosystem,
 * verifying that events are routed to correct topics with stringified keys
 * to maintain message ordering in Kafka partitions.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
public class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaEventPublisher kafkaEventPublisher;

    // Constants mirrored from Service for verification
    private static final String TOPIC_COMPLETED = "payment_completed";
    private static final String TOPIC_FAILED = "payment_failed";
    private static final String TOPIC_REFUNDED = "payment_refunded";

    /**
     * Grouping for successful event dispatch scenarios.
     * <p>
     * Verifies that the mapping between DTOs and Kafka topics is accurate
     * and that all metadata fields are preserved during the transmission.
     * </p>
     */
    @Nested
    @DisplayName("Happy Path: Event Dispatching")
    class SuccessTests {

        /**
         * Validates the Success broadcast.
         * <p>
         * <b>Significance:</b> Essential for the Order Service to finalize
         * the checkout process and trigger shipping/inventory logic.
         * </p>
         */
        @Test
        @DisplayName("Publish Success: Payment Completed")
        void publishCompleted_Success() {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    101L, 1L, 500L, 2000L, "USD", System.currentTimeMillis());

            kafkaEventPublisher.publishPaymentCompletedEvent(event);

            verify(kafkaTemplate).send(eq(TOPIC_COMPLETED), eq("101"), eq(event));
        }

        /**
         * Validates the Failure broadcast with Stripe error details.
         * <p>
         * <b>Significance:</b> Provides the Notification Service with the specific
         * error codes needed to alert the user about card declines.
         * </p>
         */
        @Test
        @DisplayName("Publish Success: Payment Failed")
        void publishFailed_Success() {
            PaymentFailedEvent event = new PaymentFailedEvent(
                    202L, 1L, 500L, 2000L, "USD",
                    "Insufficient Funds", "card_declined", System.currentTimeMillis()
            );

            kafkaEventPublisher.publishPaymentFailedEvent(event);

            verify(kafkaTemplate).send(eq(TOPIC_FAILED), eq("202"), eq(event));
        }

        /**
         * Validates the Refund broadcast.
         * <p>
         * <b>Significance:</b> Triggers Order Service to mark items as
         * 'Returned' and notifies the user of the reversal.
         * </p>
         */
        @Test
        @DisplayName("Publish Success: Payment Refunded")
        void publishPaymentRefunded_Success() {
            // Arrange: Using your full constructor signature
            PaymentRefundedEvent event = new PaymentRefundedEvent(
                    303L,           // paymentId
                    500L,           // orderId
                    "re_mock_123",  // stripeRefundId
                    2000L,          // amountRefunded
                    "USD",          // currency
                    System.currentTimeMillis() // timestamp
            );

            kafkaEventPublisher.publishPaymentRefundedEvent(event);

            // Verify topic 'payment_refunded' and key "303"
            verify(kafkaTemplate).send(eq(TOPIC_REFUNDED), eq("303"), eq(event));
        }
    }

    /**
     * Grouping for error handling logic and infrastructure resilience.
     * <p>
     * Verifies that the service degrades gracefully and does not throw
     * exceptions back to the caller if the Kafka broker is down.
     * </p>
     */
    @Nested
    @DisplayName("Broker Resilience: Error Handling")
    class FailureTests {

        /**
         * Tests that infrastructure errors are caught internally.
         * <p>
         * <b>Logic:</b> If Kafka fails, we log it but allow the main
         * execution to continue so the database transaction isn't rolled back
         * due to a non-critical messaging failure.
         * </p>
         */
        @Test
        @DisplayName("Publish Failure: Graceful degradation on Kafka Error")
        void publish_KafkaTimeout_SwallowsException() {
            PaymentCompletedEvent event = new PaymentCompletedEvent(1L, 1L, 1L, 1L, "USD", 0L);

            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenThrow(new RuntimeException("Kafka Broker Unavailable"));

            // Verification of the 'try-catch' logic in KafkaEventPublisher
            assertDoesNotThrow(() -> kafkaEventPublisher.publishPaymentCompletedEvent(event));

            verify(kafkaTemplate).send(anyString(), anyString(), any());
        }
    }
}