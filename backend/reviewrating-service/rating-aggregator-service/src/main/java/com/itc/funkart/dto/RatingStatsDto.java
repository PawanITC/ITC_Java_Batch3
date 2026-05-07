package com.itc.funkart.dto;



/**
 * DTO used by RatingSummaryServiceImpl inside the rating-aggregator-service.
 * Returned by ReviewRepository.getRatingStatsByProductId(productId).
 */
public class RatingStatsDto {

    private final Double avg;
    private final Long count;

    public RatingStatsDto(Double avg, Long count) {
        this.avg = avg;
        this.count = count;
    }

    public Double getAvg() {
        return avg;
    }

    public Long getCount() {
        return count;
    }
}

