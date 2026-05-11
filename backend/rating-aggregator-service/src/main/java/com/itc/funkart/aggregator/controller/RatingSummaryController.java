package com.itc.funkart.aggregator.controller;

import com.itc.funkart.aggregator.dto.RatingSummaryResponse;
import com.itc.funkart.aggregator.service.RatingSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rating summary controller — paths are relative after StripPrefix=2.
 *
 * <p>Gateway routes {@code /api/v1/rating-summary/**} here as {@code /rating-summary/**}.
 * All endpoints are public (no auth required) — read-only aggregate data.
 */
@RestController
@RequestMapping("/rating-summary")
@RequiredArgsConstructor
public class RatingSummaryController {

    private final RatingSummaryService summaryService;

    /**
     * GET /api/v1/rating-summary/{productId}
     * Public — returns zeroed summary if no reviews exist yet.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<RatingSummaryResponse> getSummary(@PathVariable Long productId) {
        return ResponseEntity.ok(summaryService.getSummary(productId));
    }
}
