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
            @Header("event_type") byte[] eventTypeBytes,
            Acknowledgment ack
    ) {
        String eventType = new String(eventTypeBytes, java.nio.charset.StandardCharsets.UTF_8);

        log.info("💳 [PAYMENTS_EVENTS] type={} payload={}", eventType, payload);

        try {
            switch (eventType) {

                case "PAYMENT_SUCCESS" -> {
                    PaymentCompletedEvent event =
                            objectMapper.convertValue(payload, PaymentCompletedEvent.class);

                    orderService.updateOrderStatus(event.orderId(), OrderStatus.PAID);
                }

                case "PAYMENT_FAILED" -> {
                    PaymentFailedEvent event =
                            objectMapper.convertValue(payload, PaymentFailedEvent.class);

                    orderService.updateOrderStatus(event.orderId(), OrderStatus.FAILED);
                }

                case "ORDER_REFUNDED" -> {
                    PaymentRefundedEvent event =
                            objectMapper.convertValue(payload, PaymentRefundedEvent.class);

                    orderService.updateOrderStatus(event.orderId(), OrderStatus.REFUNDED);
                }

                default -> log.warn("⚠️ Unknown event type: {}", eventType);
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("❌ PAYMENT_PROCESS failed type={} payload={}",
                    eventType, payload, e);
            // retry
        }
    }
}
