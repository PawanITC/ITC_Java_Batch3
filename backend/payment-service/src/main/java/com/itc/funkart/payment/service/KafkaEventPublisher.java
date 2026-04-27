package com.itc.funkart.payment.service;

import com.itc.funkart.payment.dto.event.PaymentCompletedEvent;
import com.itc.funkart.payment.dto.event.PaymentFailedEvent;
import com.itc.funkart.payment.dto.event.PaymentRefundedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * <h2>KafkaEventPublisher</h2>
 * <p>
 * The "Voice" of the Payment Service. 📡
 * </p>
 * <p>
 * This service acts as the Event Producer for the FunKart ecosystem. It translates
 * internal database state changes into distributed events that downstream services
 * (Order, Inventory, Notification) consume to keep the system synchronized.
 * </p>
 * <p>
 * <b>Message Ordering & Partitioning:</b>
 * To ensure <i>Sequential Consistency</i>, we use the {@code paymentId} as the Kafka Message Key.
 * This guarantees that all events related to a specific transaction are routed to the
 * <b>same Kafka Partition</b>, ensuring they are processed by consumers in the exact
 * order they were produced (e.g., COMPLETED must always arrive before REFUNDED).
 * </p>
 */
@Service
public class KafkaEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);

    /**
     * Topic for successful fund captures. Consumed by Order Service to move status to 'PAID'.
     */
    private static final String TOPIC_COMPLETED = "payment_completed";

    /**
     * Topic for rejected payments. Consumed by Order Service to move status to 'CANCELLED'.
     */
    private static final String TOPIC_FAILED = "payment_failed";

    /**
     * Topic for fund reversals. Consumed by Order Service to initiate return workflows.
     */
    private static final String TOPIC_REFUNDED = "payment_refunded";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Constructs the publisher with a configured KafkaTemplate.
     *
     * @param kafkaTemplate The Spring Kafka helper for sending messages.
     */
    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Broadcasts a successful payment event. ✓
     *
     * @param event DTO containing payment details and Stripe transaction metadata.
     */
    public void publishPaymentCompletedEvent(PaymentCompletedEvent event) {
        sendEvent(TOPIC_COMPLETED, event.paymentId(), event);
    }

    /**
     * Broadcasts a payment failure event. ✗
     *
     * @param event DTO containing error codes and failure reasons.
     */
    public void publishPaymentFailedEvent(PaymentFailedEvent event) {
        sendEvent(TOPIC_FAILED, event.paymentId(), event);
    }

    /**
     * Broadcasts a refund event. ↺
     *
     * @param event DTO containing refund IDs and amount details.
     */
    public void publishPaymentRefundedEvent(PaymentRefundedEvent event) {
        sendEvent(TOPIC_REFUNDED, event.paymentId(), event);
    }

    /**
     * Dispatches the event payload to the specified Kafka topic.
     * <p>
     * Implementation Detail: The {@code paymentId} is converted to a String key.
     * Kafka uses this key in its hashing algorithm to determine the destination partition.
     * </p>
     * * @param topic The target Kafka topic string.
     *
     * @param key   The Payment ID used for partition affinity.
     * @param event The actual DTO payload to be serialized (usually as JSON).
     */
    private void sendEvent(String topic, Long key, Object event) {
        if (key == null) {
            logger.error("🚨 Kafka Dispatch Aborted: Message Key (Payment ID) is NULL for topic [{}]", topic);
            return;
        }

        try {
            // .send() is asynchronous by default. It returns a CompletableFuture
            // if you ever need to perform logic specifically on success/failure.
            kafkaTemplate.send(topic, String.valueOf(key), event);

            logger.info("📡 Kafka [{}]: Published event for Payment ID: {}", topic, key);
        } catch (Exception ex) {
            /*
             * DEV NOTE: Reliability Strategy
             * In a production finance app, failing to send a Kafka message means
             * the Order service will never know the payment succeeded.
             * Consider implementing an 'Outbox Pattern' if 100% delivery is required.
             */
            logger.error("🚨 Kafka Critical Error: Failed to publish to [{}]. PaymentID: {}. Reason: {}",
                    topic, key, ex.getMessage());
        }
    }
}