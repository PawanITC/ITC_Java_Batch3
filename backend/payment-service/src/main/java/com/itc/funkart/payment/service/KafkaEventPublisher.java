package com.itc.funkart.payment.service;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.common.dto.event.payment.PaymentCompletedEvent;
import com.itc.funkart.common.dto.event.payment.PaymentFailedEvent;
import com.itc.funkart.common.dto.event.payment.PaymentRefundedEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * <h2>KafkaEventPublisher</h2>
 * <p>
 * Dispatches payment lifecycle events to the Kafka cluster.
 * Uses the {@code paymentId} as the message key to guarantee partition affinity
 * and sequential processing of transaction updates.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCompletedEvent(PaymentCompletedEvent event) {
        sendEvent(event.paymentId(), event, OrderEventType.PAYMENT_SUCCESS);
    }

    public void publishPaymentFailedEvent(PaymentFailedEvent event) {
        sendEvent(event.paymentId(), event, OrderEventType.PAYMENT_FAILED);
    }

    public void publishPaymentRefundedEvent(PaymentRefundedEvent event) {
        sendEvent(event.paymentId(), event, OrderEventType.ORDER_REFUNDED);
    }

    private void sendEvent(Long key, Object event, OrderEventType type) {

        if (key == null) {
            log.error("🚨 Kafka rejected NULL key for eventType={}", type);
            return;
        }

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(
                        KafkaTopics.PAYMENTS_EVENTS,
                        String.valueOf(key),
                        event
                );

        record.headers().add(
                "event_type",
                type.name().getBytes(StandardCharsets.UTF_8)
        );

        kafkaTemplate.send(record)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("❌ Kafka publish failed [{}]: {}", type, ex.getMessage());
                    } else {
                        log.info("📡 Kafka published [{}] key={} offset={}",
                                type,
                                key,
                                res.getRecordMetadata().offset());
                    }
                });
    }
}