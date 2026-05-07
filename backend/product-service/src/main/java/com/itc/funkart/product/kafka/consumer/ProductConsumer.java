package com.itc.funkart.product.kafka.consumer;

import com.itc.funkart.common.constants.messaging.KafkaGroups;
import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.product.repository.CartRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class ProductConsumer {

    private static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    private static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    private static final String ORDER_REFUNDED = "ORDER_REFUNDED";

    private final CartRepository cartRepository;

    public ProductConsumer(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENTS_EVENTS,
            groupId = KafkaGroups.PRODUCT_SERVICE_GROUP
    )
    public void onPaymentOutcome(Map<String, Object> payload) {

        Long userId = safeLong(payload.get("userId")); // ✅ FIXED
        String eventType = payload.get("event_type") != null
                ? payload.get("event_type").toString()
                : null;

        if (userId == null || eventType == null) {
            log.warn("⚠️ Invalid payment event: {}", payload);
            return;
        }

        switch (eventType) {

            case PAYMENT_SUCCESS -> {
                log.info("🛒 PAYMENT SUCCESS → clearing cart for user={}", userId);

                cartRepository.findByUserId(userId).ifPresent(cart -> {
                    cart.getItems().clear();
                    cartRepository.save(cart);
                });
            }

            case PAYMENT_FAILED -> {
                log.info("📦 PAYMENT FAILED for user={}", userId);
            }

            case ORDER_REFUNDED -> {
                log.info("🔄 REFUND for user={}", userId);
            }

            default -> log.warn("Unknown event_type={} payload={}", eventType, payload);
        }
    }

    private Long safeLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }
}