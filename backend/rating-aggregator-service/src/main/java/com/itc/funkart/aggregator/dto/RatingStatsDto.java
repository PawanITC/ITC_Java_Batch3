package com.itc.funkart.aggregator.dto;


/**
 * DTO used by RatingSummaryServiceImpl inside the rating-aggregator-service.
 * Returned by ReviewRepository.getRatingStatsByProductId(productId).
 */
public record RatingStatsDto(Double avg, Long count) {

}

