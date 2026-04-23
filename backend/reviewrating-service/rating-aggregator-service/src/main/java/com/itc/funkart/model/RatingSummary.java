package com.itc.funkart.model;


import java.util.Map;

public record RatingSummary(
        String productId,
        double averageRating,
        long totalReviews,
        Map<Integer, Long> starCounts
) {}
