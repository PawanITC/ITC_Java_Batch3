package com.itc.funkart.event;

import java.time.Instant;

public record ReviewDeletedEvent(
        String eventType,
        Long reviewId,
        Long productId,
        Long userId,
        Integer rating,
        Instant deletedAt
) {
    public static ReviewDeletedEvent of(Long reviewId, Long productId, Long userId,
                                        Integer rating) {
        return new ReviewDeletedEvent("REVIEW_DELETED", reviewId, productId, userId,
                rating, Instant.now());
    }
}
