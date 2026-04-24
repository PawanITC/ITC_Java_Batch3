package com.itc.funkart.dto;

import com.itc.funkart.entity.ProductRatingSummary;

public class ProductRatingSummaryResponse {
    private Long productId;
    private double averageRating;
    private long ratingCount;

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

    public static ProductRatingSummaryResponse from(ProductRatingSummary s) {
        ProductRatingSummaryResponse resp = new ProductRatingSummaryResponse();
        resp.productId = s.getProductId();
        resp.averageRating = s.getAverageRating();
        resp.ratingCount = s.getRatingCount();
        return resp;
    }
}
