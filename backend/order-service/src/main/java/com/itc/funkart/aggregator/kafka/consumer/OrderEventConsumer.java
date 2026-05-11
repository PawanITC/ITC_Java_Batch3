package com.itc.funkart.aggregator.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.common.constants.messaging.KafkaGroups;
import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.common.dto.event.checkout.CheckoutInitiatedEvent;
import com.itc.funkart.common.dto.event.payment.PaymentCompletedEvent;
import com.itc.funkart.common.dto.event.payment.PaymentFailedEvent;
import com.itc.funkart.common.dto.event.payment.PaymentRefundedEvent;
import com.itc.funkart.common.enums.order.OrderStatus;
import com.itc.funkart.aggregator.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // CHECKOUT_INITIATED
    // -------------------------------------------------------------------------

    @KafkaListener(
            topics = KafkaTopics.CHECKOUT_INITIATED,
            groupId = KafkaGroups.ORDER_SERVICE_GROUP
    )
    public void handleCheckoutInitiated(
            @Payload Map<String, Object> payload,
            Acknowledgment ack
    ) {
        try {
            CheckoutInitiatedEvent event =
                    objectMapper.convertValue(payload, CheckoutInitiatedEvent.class);

            log.info("📥 [CHECKOUT_INITIATED] customerId={} amount={} items={}",
                    event.customerId(),
                    event.totalAmount(),
                    event.items() != null ? event.items().size() : 0);

            orderService.processOrderInitiation(event);

            ack.acknowledge();

        } catch (Exception e) {
            log.error("❌ Failed CHECKOUT_INITIATED processing payload={}", payload, e);
            // no ack → retry
        }
    }

    // -------------------------------------------------------------------------
    // PAYMENTS_EVENTS (Payment lifecycle stream)
    // -------------------------------------------------------------------------

    @KafkaListener(
            topics = KafkaTopics.PAYMENTS_EVENTS,
            groupId = KafkaGroups.ORDER_SERVICE_GROUP
    )
    public void handlePaymentOutcome(
            @Payload Map<String, Object> payload,
            @Header(value = "event_type", required = false) byte[] eventTypeBytes,
            Acknowledgment ack
    ) {
        // event_type header may be absent on legacy or manually-produced messages —
        // fall back to the payload's eventType field so we never block the consumer.
        String eventType = eventTypeBytes != null
                ? new String(eventTypeBytes, StandardCharsets.UTF_8)
                : (String) payload.getOrDefault("eventType", "UNKNOWN");

        log.info("💳 [PAYMENTS_EVENTS] type={} orderId={}", eventType,
                payload.getOrDefault("orderId", "?"));

        try {
            switch (eventType) {

                case "PAYMENT_SUCCESS" -> {
                    PaymentCompletedEvent event =
                            objectMapper.convertValue(payload, PaymentCompletedEvent.class);
                    log.info("✅ Marking order {} as PAID", event.orderId());
                    orderService.updateOrderStatus(event.orderId(), OrderStatus.PAID);
                }

                case "PAYMENT_FAILED" -> {
                    PaymentFailedEvent event =
                            objectMapper.convertValue(payload, PaymentFailedEvent.class);
                    log.info("❌ Marking order {} as FAILED", event.orderId());
                    orderService.updateOrderStatus(event.orderId(), OrderStatus.FAILED);
                }

                case "ORDER_REFUNDED" -> {
                    PaymentRefundedEvent event =
                            objectMapper.convertValue(payload, PaymentRefundedEvent.class);
                    log.info("↩️ Marking order {} as REFUNDED", event.orderId());
                    orderService.updateOrderStatus(event.orderId(), OrderStatus.REFUNDED);
                }

                default -> log.warn("⚠️ Unrecognised payment event type '{}' — ACK'ing to unblock", eventType);
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("❌ PAYMENT_PROCESS failed type={} payload={}",
                    eventType, payload, e);
            // no ack → Kafka will redeliver
        }
    }
}
