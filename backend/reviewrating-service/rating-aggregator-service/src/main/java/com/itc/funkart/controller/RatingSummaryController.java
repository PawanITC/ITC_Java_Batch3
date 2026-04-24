package com.itc.funkart.controller;

import com.itc.funkart.model.RatingSummary;
import com.itc.funkart.service.RatingAggregationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/aggregator")
public class RatingSummaryController {

    private final RatingAggregationService aggregationService;

    public RatingSummaryController(RatingAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @GetMapping("/products/{productId}/rating-summary")
    public RatingSummary getRatingSummary(@PathVariable String productId) {
        return aggregationService.getFromCache(productId)
                .orElseGet(() -> aggregationService.recomputeAndCache(productId));
    }
}


