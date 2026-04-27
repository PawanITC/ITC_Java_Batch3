package com.itc.funkart.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_rating_summary")
public class ProductRatingSummary {

    @Id
    private Long productId;

    private Double averageRating;
    private Long ratingCount;



    public ProductRatingSummary() {}

    public ProductRatingSummary(Long productId) {
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Long getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Long ratingCount) {
        this.ratingCount = ratingCount;
    }
}
