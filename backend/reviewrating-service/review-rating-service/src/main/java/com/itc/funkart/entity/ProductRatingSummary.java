// com/itc/funkart/entity/ProductRatingSummary.java
package com.itc.funkart.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_rating_summary")
@Getter
@Setter
@NoArgsConstructor
public class ProductRatingSummary {

    @Id
    private Long productId;

    private Double averageRating = 0.0;

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

    private Long ratingCount = 0L;

    public ProductRatingSummary(Long productId) {
        this.productId = productId;
    }
}
