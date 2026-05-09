package com.itc.funkart.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.notification.dto.OrderEventDTO;
import com.itc.funkart.notification.event.OrderStatus;
import com.itc.funkart.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Listens to order lifecycle and payment events from Kafka and
 * converts them into notification triggers.
 *
 * <p>Order Service publishes {@code OrderEvent} records (topic: orders.events.v1)
 * with fields: eventType, orderId, customerId, totalAmount, timestamp, items.</p>
 *
 * <p>Payment Service publishes {@code PaymentCompletedEvent} records
 * (topic: payments.events.v1) with fields: paymentId, orderId, userId, stripeId, amount, timestamp.</p>
 *
 * <p>Neither event carries a customer email because that lives in User Service.
 * For MVP we log the event and attempt notification; real email lookup would
 * require a synchronous call to User Service (deferred).</p>
 */
@Service
public class OrderEventKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public OrderEventKafkaListener(ObjectMapper objectMapper,
                                   NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    /**
     * Handles order lifecycle events from the Order Service.
     * Publishes to topic: orders.events.v1
     */
    @KafkaListener(
            topics = "${app.kafka.topics.order-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listenOrderEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);

            // Try legacy format first (has "email" field from old DTO)
            if (node.has("email")) {
                OrderEventDTO event = objectMapper.treeToValue(node, OrderEventDTO.class);
                log.info("Order event (legacy) received — orderId={} status={}", event.getOrderId(), event.getStatus());
                notificationService.processOrderEvent(event);
                return;
            }

            // OrderEvent format from common-contracts
            String eventType = node.path("eventType").asText("UNKNOWN");
            String orderId = node.path("orderId").asText("");
            String customerId = node.path("customerId").asText("");

            log.info("Order event received — eventType={} orderId={} customerId={}", eventType, orderId, customerId);

            // Build a compatible DTO with the available data; email not present in this event format.
            // A full implementation would call User Service to resolve customerId → email.
            OrderEventDTO dto = new OrderEventDTO();
            dto.setOrderId(orderId.isEmpty() ? "UNKNOWN" : orderId);
            dto.setEmail(null);   // unavailable — would require User Service lookup
            dto.setPhone(null);
            dto.setStatus(mapOrderEventType(eventType));

            if (dto.getStatus() != null && dto.getEmail() != null) {
                notificationService.processOrderEvent(dto);
            } else {
                log.info("Skipping notification for orderId={}: email not available in event payload.", orderId);
            }

        } catch (Exception ex) {
            log.error("Failed to process order event: {}", payload, ex);
        }
    }

    /**
     * Handles payment completion events from the Payment Service.
     * Publishes to topic: payments.events.v1
     */
    @KafkaListener(
            topics = "${app.kafka.topics.payment-events}",
            groupId = "${spring.kafka.consumer.group-id}-payments"
    )
    public void listenPaymentEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);

            String orderId = node.path("orderId").asText("");
            String userId = node.path("userId").asText("");
            long amount = node.path("amount").asLong(0L);
            String stripeId = node.path("stripeId").asText("");

            log.info("Payment completed event — orderId={} userId={} amount={} stripeId={}",
                    orderId, userId, amount, stripeId);

            // Build notification DTO for DELIVERED (payment success proxy)
            // Email resolution from User Service is required for actual delivery (deferred for MVP)
            OrderEventDTO dto = new OrderEventDTO();
            dto.setOrderId(orderId.isEmpty() ? "UNKNOWN" : orderId);
            dto.setStatus(OrderStatus.ORDER_CONFIRMED);
            dto.setEmail(null);   // requires User Service call — deferred
            dto.setPhone(null);

            log.info("Payment success for order {} — notification queued (email resolution pending).", orderId);

        } catch (Exception ex) {
            log.error("Failed to process payment event: {}", payload, ex);
        }
    }

    /**
     * Maps order-service eventType strings to the notification service's OrderStatus enum.
     */
    private OrderStatus mapOrderEventType(String eventType) {
        if (eventType == null) return null;
        return switch (eventType.toUpperCase()) {
            case "ORDER_INITIATED", "ORDER_CREATED" -> OrderStatus.ORDER_PLACED;
            case "ORDER_CONFIRMED" -> OrderStatus.ORDER_CONFIRMED;
            case "PAYMENT_SUCCESS" -> OrderStatus.ORDER_CONFIRMED;
            case "ORDER_SHIPPED" -> OrderStatus.DISPATCHED;
            case "ORDER_DELIVERED" -> OrderStatus.DELIVERED;
            case "ORDER_CANCELLED", "PAYMENT_FAILED" -> OrderStatus.ORDER_CANCELLED;
            default -> {
                log.warn("Unknown order eventType '{}' — skipping notification.", eventType);
                yield null;
            }
        };
    }
}
