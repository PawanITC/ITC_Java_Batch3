 package com.itc.funkart.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_reviews_product", columnList = "productId"),
        @Index(name = "idx_reviews_user_product", columnList = "userId,productId", unique = true)
})
public class Review {

    @Id
    @GeneratedValue
    private UUID id;

    private String productId;
    private String userId;
    private int rating;


    @Column(length = 4000)
    private String reviewText;

    private Instant createdAt;
    private Instant updatedAt;

    // getters/setters omitted for brevity
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
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

    // --- Add these lifecycle hooks here ---
    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Review(Long productId, Long userId, Instant createdAt) {
        this.productId = productId != null ? productId.toString() : null;
        this.userId = userId != null ? userId.toString() : null;
        this.createdAt = createdAt;
        this.updatedAt = createdAt; // optional: set updatedAt same as createdAt
    }


}
