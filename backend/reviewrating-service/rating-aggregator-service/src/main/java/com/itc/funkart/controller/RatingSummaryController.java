package com.itc.funkart.controller;

import com.itc.funkart.dto.ProductRatingSummaryResponse;
import com.itc.funkart.model.ProductRatingSummary;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// NOTE: /rating-summary — gateway strips /api/v1 via StripPrefix=2 before forwarding here
@RestController
@RequestMapping("/rating-summary")
@RequiredArgsConstructor
public class RatingSummaryController {

    private final ProductRatingSummaryRepository summaryRepository;

    @GetMapping("/{productId}")
    public ProductRatingSummaryResponse getSummary(@PathVariable Long productId) {
        ProductRatingSummary s = summaryRepository.findById(productId)
                .orElse(new ProductRatingSummary(productId));
        return new ProductRatingSummaryResponse(
                s.getProductId(),
                s.getAverageRating() != null ? s.getAverageRating() : 0.0,
                s.getRatingCount() != null ? s.getRatingCount() : 0L
        );
    }
}
