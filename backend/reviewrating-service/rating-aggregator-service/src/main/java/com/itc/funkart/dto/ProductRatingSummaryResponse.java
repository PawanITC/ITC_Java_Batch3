package com.itc.funkart.dto;



public class ProductRatingSummaryResponse {

    private Long productId;
    private Double averageRating;
    private Long ratingCount;

    public ProductRatingSummaryResponse(Long productId, Double averageRating, Long ratingCount) {
        this.productId = productId;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
    }

    public Long getProductId() {
        return productId;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public Long getRatingCount() {
        return ratingCount;
    }
}

