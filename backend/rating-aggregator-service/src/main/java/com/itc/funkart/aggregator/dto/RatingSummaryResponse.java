package com.itc.funkart.aggregator.dto;

import com.itc.funkart.aggregator.entity.RatingSummary;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RatingSummaryResponse {

    private Long productId;
    private int totalReviews;
    private double averageRating;
    private int oneStar;
    private int twoStar;
    private int threeStar;
    private int fourStar;
    private int fiveStar;

    public static RatingSummaryResponse from(RatingSummary s) {
        return RatingSummaryResponse.builder()
                .productId(s.getProductId())
                .totalReviews(s.getTotalReviews())
                .averageRating(s.getAverageRating())
                .oneStar(s.getOneStar())
                .twoStar(s.getTwoStar())
                .threeStar(s.getThreeStar())
                .fourStar(s.getFourStar())
                .fiveStar(s.getFiveStar())
                .build();
    }

    /** Returns a zeroed summary for products with no reviews yet. */
    public static RatingSummaryResponse empty(Long productId) {
        return RatingSummaryResponse.builder()
                .productId(productId)
                .totalReviews(0)
                .averageRating(0.0)
                .oneStar(0).twoStar(0).threeStar(0).fourStar(0).fiveStar(0)
                .build();
    }
}
