package com.itc.funkart.aggregator.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.common.dto.event.checkout.CheckoutInitiatedEvent;
import com.itc.funkart.common.dto.event.payment.PaymentCompletedEvent;
import com.itc.funkart.common.dto.event.payment.PaymentFailedEvent;
import com.itc.funkart.common.dto.event.payment.PaymentRefundedEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.common.enums.order.OrderStatus;
import com.itc.funkart.aggregator.kafka.consumer.OrderEventConsumer;
import com.itc.funkart.aggregator.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>OrderEventConsumerTest</h2>
 *
 * <p>Rewrote from commented-out version — the original used {@code OrderInitiatedEvent}
 * and {@code @KafkaHandler} routing, both of which no longer exist. The consumer was
 * refactored to receive raw {@code Map<String, Object>} payloads deserialized via
 * {@link ObjectMapper}.</p>
 *
 * <p>Key contracts tested:</p>
 * <ul>
 *   <li>Success path: deserialize → service → {@code ack.acknowledge()}</li>
 *   <li>Service exception → NO ack (Kafka retry)</li>
 *   <li>Deserialization failure → NO ack (Kafka retry)</li>
 *   <li>Payment event routing by {@code event_type} header value</li>
 *   <li>Unknown event types handled without service calls</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class OrderEventConsumerTest {

    @Mock
    private OrderService orderService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private Acknowledgment ack;
    @InjectMocks
    private OrderEventConsumer consumer;

    // =========================================================
    // handleCheckoutInitiated
    // =========================================================

    @Nested
    @DisplayName("handleCheckoutInitiated")
    class CheckoutInitiatedTests {

        @Test
        @DisplayName("Success: deserializes, calls service, acknowledges")
        void success() {
            Map<String, Object> payload = Map.of("customerId", 100L);
            CheckoutInitiatedEvent event = new CheckoutInitiatedEvent(
                    OrderEventType.ORDER_INITIATED, 100L, new BigDecimal("250.00"),
                    List.of(), "usd", null);
            when(objectMapper.convertValue(payload, CheckoutInitiatedEvent.class)).thenReturn(event);

            consumer.handleCheckoutInitiated(payload, ack);

            verify(orderService).processOrderInitiation(event);
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("Service throws: does NOT acknowledge (forces Kafka retry)")
        void serviceException_noAck() {
            Map<String, Object> payload = Map.of("customerId", 100L);
            CheckoutInitiatedEvent event = new CheckoutInitiatedEvent(
                    OrderEventType.ORDER_INITIATED, 100L, BigDecimal.ZERO, List.of(), "usd", null);
            when(objectMapper.convertValue(payload, CheckoutInitiatedEvent.class)).thenReturn(event);
            doThrow(new RuntimeException("DB timeout")).when(orderService).processOrderInitiation(any());

            consumer.handleCheckoutInitiated(payload, ack);

            verify(ack, never()).acknowledge();
        }

        @Test
        @DisplayName("Deserialization fails: does NOT acknowledge (forces Kafka retry)")
        void deserializationFailure_noAck() {
            Map<String, Object> payload = Map.of("bad", true);
            when(objectMapper.convertValue(payload, CheckoutInitiatedEvent.class))
                    .thenThrow(new IllegalArgumentException("bad payload"));

            consumer.handleCheckoutInitiated(payload, ack);

            verifyNoInteractions(orderService);
            verify(ack, never()).acknowledge();
        }
    }

    // =========================================================
    // handlePaymentOutcome
    // =========================================================

    @Nested
    @DisplayName("handlePaymentOutcome — routing by event_type header")
    class PaymentOutcomeTests {

        private byte[] h(String type) {
            return type.getBytes(StandardCharsets.UTF_8);
        }

        @Test
        @DisplayName("PAYMENT_SUCCESS → updateOrderStatus(orderId, PAID)")
        void paymentSuccess() {
            Map<String, Object> payload = Map.of("orderId", 55L);
            PaymentCompletedEvent ev = PaymentCompletedEvent.builder()
                    .paymentId(1L).orderId(55L).stripeId("pi_1").amount(5000L).build();
            when(objectMapper.convertValue(payload, PaymentCompletedEvent.class)).thenReturn(ev);

            consumer.handlePaymentOutcome(payload, h("PAYMENT_SUCCESS"), ack);

            verify(orderService).updateOrderStatus(55L, OrderStatus.PAID);
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("PAYMENT_FAILED → updateOrderStatus(orderId, FAILED)")
        void paymentFailed() {
            Map<String, Object> payload = Map.of("orderId", 66L);
            PaymentFailedEvent ev = PaymentFailedEvent.builder()
                    .paymentId(2L).orderId(66L).stripeId("pi_2").build();
            when(objectMapper.convertValue(payload, PaymentFailedEvent.class)).thenReturn(ev);

            consumer.handlePaymentOutcome(payload, h("PAYMENT_FAILED"), ack);

            verify(orderService).updateOrderStatus(66L, OrderStatus.FAILED);
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("ORDER_REFUNDED → updateOrderStatus(orderId, REFUNDED)")
        void orderRefunded() {
            Map<String, Object> payload = Map.of("orderId", 77L);
            PaymentRefundedEvent ev = PaymentRefundedEvent.builder()
                    .paymentId(3L).orderId(77L).stripeRefundId("re_1")
                    .amountRefunded(5000L).currency("usd").build();
            when(objectMapper.convertValue(payload, PaymentRefundedEvent.class)).thenReturn(ev);

            consumer.handlePaymentOutcome(payload, h("ORDER_REFUNDED"), ack);

            verify(orderService).updateOrderStatus(77L, OrderStatus.REFUNDED);
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("Unknown type: no service call, still acknowledges")
        void unknownType_acknowledgesWithoutService() {
            consumer.handlePaymentOutcome(Map.of(), h("UNKNOWN_TYPE"), ack);

            verifyNoInteractions(orderService);
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("Service throws: does NOT acknowledge (forces Kafka retry)")
        void serviceException_noAck() {
            Map<String, Object> payload = Map.of("orderId", 99L);
            PaymentCompletedEvent ev = PaymentCompletedEvent.builder()
                    .paymentId(4L).orderId(99L).stripeId("pi_4").amount(100L).build();
            when(objectMapper.convertValue(payload, PaymentCompletedEvent.class)).thenReturn(ev);
            doThrow(new RuntimeException("CB open")).when(orderService).updateOrderStatus(any(), any());

            consumer.handlePaymentOutcome(payload, h("PAYMENT_SUCCESS"), ack);

            verify(orderService).updateOrderStatus(99L, OrderStatus.PAID);
            verify(ack, never()).acknowledge();
        }

        @Test
        @DisplayName("Deserialization fails: does NOT acknowledge (forces Kafka retry)")
        void deserializationFailure_noAck() {
            Map<String, Object> payload = Map.of("bad", "data");
            when(objectMapper.convertValue(payload, PaymentCompletedEvent.class))
                    .thenThrow(new IllegalArgumentException("bad payload"));

            consumer.handlePaymentOutcome(payload, h("PAYMENT_SUCCESS"), ack);

            verifyNoInteractions(orderService);
            verify(ack, never()).acknowledge();
        }
    }
}
