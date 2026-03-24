package com.itc.funkart.payment.service;

import com.itc.funkart.payment.dto.event.PaymentCompletedEvent;
import com.itc.funkart.payment.dto.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish payment completed event to Kafka
     */
    public void publishPaymentCompletedEvent(PaymentCompletedEvent event) {
        try {
            kafkaTemplate.send("payment-completed", event.paymentId().toString(), event);
            logger.info("✓ Published payment-completed event for payment: {}", event.paymentId());
        } catch (Exception ex) {
            logger.error("✗ Failed to publish payment-completed event: {}", ex.getMessage());
        }
    }

    /**
     * Publish payment failed event to Kafka
     */
    public void publishPaymentFailedEvent(PaymentFailedEvent event) {
        try {
            kafkaTemplate.send("payment-failed", event.getPaymentId().toString(), event);
            logger.info("✓ Published payment-failed event for payment: {}", event.getPaymentId());
        } catch (Exception ex) {
            logger.error("✗ Failed to publish payment-failed event: {}", ex.getMessage());
        }
    }
}