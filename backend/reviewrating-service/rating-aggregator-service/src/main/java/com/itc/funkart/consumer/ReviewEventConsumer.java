package com.itc.funkart.consumer;

import com.itc.funkart.service.RatingSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Listens for review events published by review-rating-service.
 * Each event carries at minimum: { eventType, productId, ... }
 * On any mutation event, triggers a summary recalculation from the shared DB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEventConsumer {

    private final RatingSummaryService ratingSummaryService;

    @KafkaListener(
            topics = "${app.kafka.topic.reviews:review.events.v1}",
            groupId = "${spring.kafka.consumer.group-id:rating-aggregator-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            Object rawProductId = event.get("productId");

            if (rawProductId == null) {
                log.warn("Received review event with no productId — skipping. type={}", eventType);
                return;
            }

            Long productId = rawProductId instanceof Number n
                    ? n.longValue()
                    : Long.parseLong(rawProductId.toString());

            log.debug("Received {} for productId={}", eventType, productId);
            ratingSummaryService.recalculateSummary(productId);

        } catch (Exception e) {
            log.error("Failed to process review event: {}", e.getMessage(), e);
        }
    }
}
