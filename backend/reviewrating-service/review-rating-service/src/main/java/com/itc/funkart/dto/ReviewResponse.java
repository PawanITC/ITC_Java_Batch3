package com.itc.funkart.dto;


import com.itc.funkart.entity.Review;

import java.time.Instant;
import java.util.UUID;

public class ReviewResponse {
    private UUID id;
    private Long productId;
    private Long userId;
     int rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;

    public ReviewResponse() {
    }

    public ReviewResponse(UUID id, Long productId, Long userId,
                          int rating, String comment,
                          Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.productId = productId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                Long.valueOf(review.getProductId()),
                Long.valueOf(review.getUserId()),
                review.getRating(),
                review.getReviewText(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

}

