package com.itc.funkart.aggregator.kafka;

import com.itc.funkart.aggregator.entity.RatingSummary;
import com.itc.funkart.aggregator.repository.RatingSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes {@code review.events.v1} messages and maintains the
 * {@link RatingSummary} aggregate per product.
 *
 * <p>All exceptions are swallowed so that the consumer offset advances
 * even on bad/malformed events — bad events are logged and dropped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventConsumer {

    private final RatingSummaryRepository summaryRepository;

    @KafkaListener(topics = "${app.kafka.topic.reviews}", groupId = "${spring.kafka.consumer.group-id}")
    public void onReviewEvent(Map<String, Object> event,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            String eventType = (String) event.get("eventType");
            Object productIdRaw = event.get("productId");
            Object ratingRaw   = event.get("rating");

            if (productIdRaw == null) {
                log.warn("[{}] Skipping event — missing productId: {}", topic, event);
                return;
            }

            long productId = ((Number) productIdRaw).longValue();
            int  rating    = ratingRaw != null ? ((Number) ratingRaw).intValue() : 0;

            if ("REVIEW_CREATED".equals(eventType)) {
                handleCreated(productId, rating);
            } else if ("REVIEW_DELETED".equals(eventType)) {
                handleDeleted(productId, rating);
            } else {
                log.warn("[{}] Unknown eventType '{}' — skipping", topic, eventType);
            }

        } catch (Exception ex) {
            log.error("Failed to process review event — swallowing to advance offset: {}", ex.getMessage(), ex);
        }
    }

    // ─── private helpers ─────────────────────────────────────────────────────

    private void handleCreated(long productId, int rating) {
        RatingSummary summary = summaryRepository.findById(productId)
                .orElseGet(() -> RatingSummary.builder()
                        .productId(productId)
                        .totalReviews(0).sumRatings(0).averageRating(0.0)
                        .oneStar(0).twoStar(0).threeStar(0).fourStar(0).fiveStar(0)
                        .build());

        summary.setTotalReviews(summary.getTotalReviews() + 1);
        summary.setSumRatings(summary.getSumRatings() + rating);
        incrementStar(summary, rating, 1);
        summary.setAverageRating((double) summary.getSumRatings() / summary.getTotalReviews());

        summaryRepository.save(summary);
        log.debug("REVIEW_CREATED applied for productId={}, new avg={}", productId, summary.getAverageRating());
    }

    private void handleDeleted(long productId, int rating) {
        summaryRepository.findById(productId).ifPresent(summary -> {
            int newTotal = Math.max(0, summary.getTotalReviews() - 1);
            int newSum   = Math.max(0, summary.getSumRatings() - rating);

            summary.setTotalReviews(newTotal);
            summary.setSumRatings(newSum);
            incrementStar(summary, rating, -1);
            summary.setAverageRating(newTotal > 0 ? (double) newSum / newTotal : 0.0);

            summaryRepository.save(summary);
            log.debug("REVIEW_DELETED applied for productId={}, new avg={}", productId, summary.getAverageRating());
        });
    }

    private void incrementStar(RatingSummary s, int rating, int delta) {
        switch (rating) {
            case 1 -> s.setOneStar(Math.max(0, s.getOneStar() + delta));
            case 2 -> s.setTwoStar(Math.max(0, s.getTwoStar() + delta));
            case 3 -> s.setThreeStar(Math.max(0, s.getThreeStar() + delta));
            case 4 -> s.setFourStar(Math.max(0, s.getFourStar() + delta));
            case 5 -> s.setFiveStar(Math.max(0, s.getFiveStar() + delta));
            default -> log.warn("Unexpected rating value: {}", rating);
        }
    }
}
