// com/itc/funkart/dto/ReviewResponse.java
package com.itc.funkart.dto;

import com.itc.funkart.entity.Review;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        Integer rating,
        String comment,
        Instant createdAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getProductId(),
                r.getUserId(),
                r.getRating(),
                r.getReviewText(),
                r.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .toInstant()
        );
    }
}