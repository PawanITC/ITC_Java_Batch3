package com.itc.funkart.aggregator.dto;


import lombok.Getter;

@Getter
public class ProductRatingSummaryResponse {

    private Long productId;
    private Double averageRating;
    private Long ratingCount;

    public ProductRatingSummaryResponse(Long productId, Double averageRating, Long ratingCount) {
        this.productId = productId;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
    }

}

