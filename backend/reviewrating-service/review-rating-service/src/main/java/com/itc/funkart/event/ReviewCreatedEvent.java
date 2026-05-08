package com.itc.funkart.event;

import java.time.Instant;

public record ReviewCreatedEvent(
        String eventType,
        Long reviewId,
        Long productId,
        Long userId,
        Integer rating,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReviewCreatedEvent of(Long reviewId, Long productId, Long userId,
                                        Integer rating, String comment,
                                        Instant createdAt, Instant updatedAt) {
        return new ReviewCreatedEvent("REVIEW_CREATED", reviewId, productId, userId,
                rating, comment, createdAt, updatedAt);
    }
}
