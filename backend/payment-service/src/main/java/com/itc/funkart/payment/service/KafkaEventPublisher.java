package com.itc.funkart.payment.service;

import com.itc.funkart.payment.dto.event.PaymentCompletedEvent;
import com.itc.funkart.payment.dto.event.PaymentFailedEvent;
import com.itc.funkart.payment.dto.event.PaymentRefundedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * The "Voice" of the Payment Service. 📡
 * <p>
 * This service broadcasts critical payment state changes to the rest of the
 * FunKart ecosystem (Order Service, Inventory, etc.).
 * </p>
 * <p>
 * <b>Ordering Guarantee:</b> We use {@code paymentId} as the Kafka Message Key.
 * This ensures all events for a single transaction (Success -> Refund)
 * are processed in the correct sequence by consumers.
 * </p>
 */
@Service
public class KafkaEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);

    // Standardized Event Contracts for the FunKart ecosystem.
    // These topic names are shared with the Order service and other consumers.
    private static final String TOPIC_COMPLETED = "payment_completed";
    private static final String TOPIC_FAILED = "payment_failed";
    private static final String TOPIC_REFUNDED = "payment_refunded";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Broadcasts that funds have been successfully captured. ✓
     */
    public void publishPaymentCompletedEvent(PaymentCompletedEvent event) {
        sendEvent(TOPIC_COMPLETED, event.paymentId(), event);
    }

    /**
     * Broadcasts that a payment attempt was rejected by the provider. ✗
     */
    public void publishPaymentFailedEvent(PaymentFailedEvent event) {
        sendEvent(TOPIC_FAILED, event.paymentId(), event);
    }

    /**
     * Broadcasts that money has been returned to the customer. ↺
     */
    public void publishPaymentRefundedEvent(PaymentRefundedEvent event) {
        sendEvent(TOPIC_REFUNDED, event.paymentId(), event);
    }

    /**
     * Core helper to dispatch events to the Kafka cluster.
     * * @param topic The target Kafka topic
     * @param key   The Payment ID (used for partition stickiness)
     * @param event The DTO payload (serialized as JSON)
     */
    private void sendEvent(String topic, Long key, Object event) {
        try {
            // We convert the Long key to String to ensure Kafka partitions correctly
            kafkaTemplate.send(topic, key.toString(), event);

            logger.info("📡 Kafka [{}]: Published event for Payment ID: {}", topic, key);
        } catch (Exception ex) {
            // Log the error but don't crash the main thread.
            // In high-stakes finance, you might want a 'Dead Letter Queue' or retry here.
            logger.error("🚨 Kafka Error: Failed to publish to [{}]. Reason: {}", topic, ex.getMessage());
        }
    }
}