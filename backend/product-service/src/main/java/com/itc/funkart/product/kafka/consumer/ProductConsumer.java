package com.itc.funkart.product.kafka.consumer;

import com.itc.funkart.common.constants.messaging.KafkaGroups;
import com.itc.funkart.common.constants.messaging.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * <h2>ProductConsumer</h2>
 * <p>
 * Consumes payment lifecycle events from {@code PAYMENT_PROCESS}
 * to update inventory state in response to payment outcomes.
 *
 * <p>
 * Uses Kafka header-driven {@code event_type} emitted by Payment Service.
 * This keeps Product Service decoupled from Payment/Order domain enums.
 * </p>
 */
@Service
@Slf4j
public class ProductConsumer {

    // -------------------------------------------------------------------------
    // EVENT TYPE CONSTANTS (local contract boundary)
    // -------------------------------------------------------------------------

    private static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    private static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    private static final String ORDER_REFUNDED = "ORDER_REFUNDED";

    @KafkaListener(
            topics = KafkaTopics.PAYMENTS_EVENTS,
            groupId = KafkaGroups.PRODUCT_SERVICE_GROUP
    )
    public void onPaymentOutcome(Map<String, Object> payload) {

        Long orderId = safeLong(payload.get("order_id"));

        String eventType = payload.get("event_type") != null
                ? payload.get("event_type").toString()
                : null;

        if (orderId == null || eventType == null) {
            log.warn("⚠️ [INVENTORY] Invalid payment event received. payload={}", payload);
            return;
        }

        switch (eventType) {

            case PAYMENT_FAILED -> {
                log.info("📦 [INVENTORY] PAYMENT FAILED for Order {} — releasing reserved stock.", orderId);
                // InventoryService.releaseReservation(orderId);
            }

            case ORDER_REFUNDED -> {
                log.info("📦 [INVENTORY] PAYMENT REFUNDED for Order {} — restoring stock.", orderId);
                // InventoryService.restoreStock(orderId);
            }

            case PAYMENT_SUCCESS -> {
                log.info("📦 [INVENTORY] PAYMENT SUCCESS for Order {} — finalising stock deduction.", orderId);
                // InventoryService.confirmDeduction(orderId);
            }

            default -> {
                log.warn("⚠️ [INVENTORY] Unknown event_type={} for orderId={}", eventType, orderId);
            }
        }
    }

    private Long safeLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.longValue();
        return Long.valueOf(obj.toString().split("\\.")[0]);
    }
}