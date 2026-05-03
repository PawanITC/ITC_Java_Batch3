package com.itc.funkart.kafka.consumer;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
import com.itc.funkart.common.dto.event.order.OrderInitiatedEvent;
import com.itc.funkart.common.enums.order.OrderStatus;
import com.itc.funkart.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * <h2>Order Event Consumer</h2>
 * <p>
 * This component is the "Listener" of the Order Service. It watches the Kafka
 * {@code orders} topic and reacts whenever an event is detected.
 * </p>
 *
 * <h3>How it works:</h3>
 * <ul>
 *   <li><b>Type-Safe Routing:</b> Using {@link KafkaHandler}, Spring automatically
 *   routes the message to the correct method based on its Java class type.</li>
 *   <li><b>Idempotency:</b> We use a {@code groupId} to ensure that if we scale
 *   this service, only one instance processes each specific event.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@KafkaListener(topics = KafkaTopics.ORDERS, groupId = "order-service-group")
public class OrderEventConsumer {

    private final OrderService orderService;

    /**
     * <h3>Handle Order Initiation</h3>
     * <p>Triggered when a user clicks 'Checkout' in the Cart service.
     * This method starts the actual creation of the order record in our DB.</p>
     */
    @KafkaHandler
    public void handleOrderInitiated(OrderInitiatedEvent event) {
        log.info("📥 [OrderInitiatedEvent] Received for User: {} | Amount: {}",
                event.userId(), event.totalAmount());

        try {
            orderService.processOrderInitiation(event);
        } catch (Exception e) {
            log.error("❌ Failed to process initiation for User {}: {}",
                    event.userId(), e.getMessage());
        }
    }

    /**
     * <h3>Handle Order Cancellation</h3>
     * <p>Triggered when an order is voided. This updates our local RDBMS
     * status to ensure consistency across the system.</p>
     */
    @KafkaHandler
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("🚫 [OrderCancelledEvent] Received for Order ID: {}", event.orderId());

        try {
            orderService.updateOrderStatus(event.orderId(), OrderStatus.CANCELLED);
            log.info("✅ Order {} status updated to CANCELLED", event.orderId());
        } catch (Exception e) {
            log.error("❌ Failed to update cancellation for Order {}: {}",
                    event.orderId(), e.getMessage());
        }
    }

    /**
     * Catch-all for unknown message types to prevent the consumer from
     * getting stuck in an infinite retry loop on unparseable data.
     */
    @KafkaHandler(isDefault = true)
    public void handleUnknown(Object event) {
        log.warn("⚠️ Received unknown event type in Order Topic: {}",
                event.getClass().getSimpleName());
    }
}