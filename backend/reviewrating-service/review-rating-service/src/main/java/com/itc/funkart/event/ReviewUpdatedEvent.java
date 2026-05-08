package com.itc.funkart.event;

import java.time.Instant;

public record ReviewUpdatedEvent(
        String eventType,
        Long reviewId,
        Long productId,
        Long userId,
        Integer rating,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReviewUpdatedEvent of(Long reviewId, Long productId, Long userId,
                                        Integer rating, String comment,
                                        Instant createdAt, Instant updatedAt) {
        return new ReviewUpdatedEvent("REVIEW_UPDATED", reviewId, productId, userId,
                rating, comment, createdAt, updatedAt);
    }
}
