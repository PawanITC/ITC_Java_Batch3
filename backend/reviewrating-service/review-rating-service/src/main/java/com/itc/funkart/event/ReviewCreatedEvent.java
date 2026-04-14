package com.itc.funkart.event;



import java.time.Instant;
import java.util.UUID;

public class ReviewCreatedEvent {

    private Long reviewId;
    private Long productId;
    private Long userId;

    private int rating;
    private String comment;
    private Instant createdAt;

    public ReviewCreatedEvent() {
    }



    public ReviewCreatedEvent(Long id,
                              Long productId,
                              Long userId,
                              int rating,
                              String comment,
                              Instant createdAt,
                              Instant updatedAt) {

        this.reviewId = Long.valueOf(id != null ? id.toString() : UUID.randomUUID().toString());
        this.productId = productId != null ? Long.valueOf(productId.toString()) : null;
        this.userId = userId != null ? Long.valueOf(userId.toString()) : null;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Long getReviewId() {
        return reviewId;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getUserId() {
        return userId;
    }

    public int getRating() {
        return rating;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
