package com.itc.funkart.client;



public class RatingResponse {

    private String productId;
    private double averageRating;
    private long ratingCount;
    private boolean fallback;

    public RatingResponse() {
    }

    public RatingResponse(String productId, double averageRating, long ratingCount, boolean fallback) {
        this.productId = productId;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
        this.fallback = fallback;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
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

    public boolean isFallback() {
        return fallback;
    }

    public void setFallback(boolean fallback) {
        this.fallback = fallback;
    }
}
