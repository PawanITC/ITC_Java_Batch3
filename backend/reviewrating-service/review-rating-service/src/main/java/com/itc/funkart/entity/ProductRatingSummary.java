package com.itc.funkart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "product_rating_summary")
public class ProductRatingSummary {


    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "average_rating", nullable = false)
    private double averageRating;

    @Column(name = "rating_count", nullable = false)
    private long ratingCount;

    // getters/setters, constructors



    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public long getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(long ratingCount) {
        this.ratingCount = ratingCount;
    }
}
