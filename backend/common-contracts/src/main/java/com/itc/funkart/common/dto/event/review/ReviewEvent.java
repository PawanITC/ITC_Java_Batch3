package com.itc.funkart.common.dto.event.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka event published to {@code review.events.v1} by the review-rating-service.
 *
 * <p>Consumed by the rating-aggregator-service to maintain per-product rating summaries.
 *
 * <ul>
 *   <li>{@code REVIEW_CREATED} — new review submitted; include rating to update aggregate.</li>
 *   <li>{@code REVIEW_DELETED} — review removed by moderator; include original rating to reverse.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEvent {

    /**
     * Event type: REVIEW_CREATED or REVIEW_DELETED.
     */
    private String eventType;

    private Long reviewId;

    private Long productId;

    private Long userId;

    /**
     * Star rating 1–5. Used by aggregator to update or reverse the summary.
     */
    private int rating;

    private LocalDateTime timestamp;
}
