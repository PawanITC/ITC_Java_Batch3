package com.itc.funkart.payment.service;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.common.dto.event.payment.PaymentCompletedEvent;
import com.itc.funkart.common.dto.event.payment.PaymentFailedEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>KafkaEventPublisherTest</h2>
 * Validates the dispatching of payment lifecycle events using Record Builders
 * and Enum-based header classification.
 */
@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaEventPublisher kafkaEventPublisher;

    @Nested
    @DisplayName("Topic Routing & Header Validation")
    class SuccessTests {

        @Test
        @DisplayName("Publish Completed: Verify Builder usage and PAYMENT_SUCCESS header")
        void publishCompleted_Success() {
            // GIVEN: Using the new @Builder pattern
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .paymentId(101L)
                    .orderId(1L)
                    .stripeId("pi_success_123")
                    .amount(2000L)
                    .build(); // Timestamp handled by canonical constructor

            when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(new CompletableFuture<>());

            // WHEN
            kafkaEventPublisher.publishPaymentCompletedEvent(event);

            // THEN
            ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
            verify(kafkaTemplate).send(captor.capture());

            ProducerRecord<String, Object> record = captor.getValue();
            assertEquals(KafkaTopics.PAYMENTS_EVENTS, record.topic());
            assertEquals("101", record.key());

            // Verify Header
            var header = record.headers().lastHeader("event_type");
            assertNotNull(header);
            assertEquals(OrderEventType.PAYMENT_SUCCESS.name(), new String(header.value()));

            // Verify Timestamp was generated if null in builder
            assertNotNull(((PaymentCompletedEvent) record.value()).timestamp());
        }

        @Test
        @DisplayName("Publish Failed: Verify PAYMENT_FAILED header")
        void publishFailed_Success() {
            // GIVEN
            PaymentFailedEvent event = PaymentFailedEvent.builder()
                    .paymentId(202L)
                    .orderId(1L)
                    .stripeId("pi_failed_456")
                    .build();

            when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(new CompletableFuture<>());

            // WHEN
            kafkaEventPublisher.publishPaymentFailedEvent(event);

            // THEN
            ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
            verify(kafkaTemplate).send(captor.capture());

            assertEquals("202", captor.getValue().key());
            assertEquals(OrderEventType.PAYMENT_FAILED.name(),
                    new String(captor.getValue().headers().lastHeader("event_type").value()));
        }
    }

    @Nested
    @DisplayName("Edge Case Validation")
    class EdgeCaseTests {

        @Test
        @DisplayName("Guard Clause: Should abort if paymentId is null in Builder")
        void sendEvent_NullKey_Aborts() {
            // GIVEN: Builder without paymentId
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .orderId(1L)
                    .build();

            // WHEN
            kafkaEventPublisher.publishPaymentCompletedEvent(event);

            // THEN
            verify(kafkaTemplate, never()).send((ProducerRecord<String, Object>) any());
        }

        @Test
        @DisplayName("Partitioning: Verify Stringified key conversion for long IDs")
        void sendEvent_VerifiesStringKey() {
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .paymentId(99999L)
                    .build();

            when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(new CompletableFuture<>());

            // WHEN
            kafkaEventPublisher.publishPaymentCompletedEvent(event);

            // THEN
            ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
            verify(kafkaTemplate).send(captor.capture());
            assertEquals("99999", captor.getValue().key());
        }
    }
}