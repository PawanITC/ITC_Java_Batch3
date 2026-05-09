package com.itc.funkart.review.kafka;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.common.dto.event.review.ReviewEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishReviewCreated(Long reviewId, Long productId, Long userId, int rating) {
        publish("REVIEW_CREATED", reviewId, productId, userId, rating);
    }

    public void publishReviewDeleted(Long reviewId, Long productId, Long userId, int rating) {
        publish("REVIEW_DELETED", reviewId, productId, userId, rating);
    }

    private void publish(String eventType, Long reviewId, Long productId, Long userId, int rating) {
        ReviewEvent event = ReviewEvent.builder()
                .eventType(eventType)
                .reviewId(reviewId)
                .productId(productId)
                .userId(userId)
                .rating(rating)
                .timestamp(LocalDateTime.now())
                .build();

        var future = kafkaTemplate.send(KafkaTopics.REVIEWS, String.valueOf(productId), event);
        if (future != null) {
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish {} for reviewId={}: {}", eventType, reviewId, ex.getMessage());
                } else {
                    log.debug("Published {} for reviewId={}, productId={}", eventType, reviewId, productId);
                }
            });
        }
    }
}
