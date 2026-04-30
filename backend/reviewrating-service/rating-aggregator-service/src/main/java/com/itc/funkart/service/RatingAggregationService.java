package com.itc.funkart.service;

import com.itc.funkart.model.ProductRatingSummary;

import java.util.Optional;

public interface RatingAggregationService {

    void handleCreated(Long productId, int rating);

    void handleUpdated(Long productId);

    void handleDeleted(Long productId);

    Optional<ProductRatingSummary> getFromCache(Long productId);

    ProductRatingSummary recomputeAndCache(Long productId);
}
