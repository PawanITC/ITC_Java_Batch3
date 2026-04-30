package com.itc.funkart.controller;




import com.itc.funkart.dto.ProductRatingSummaryResponse;

import com.itc.funkart.model.ProductRatingSummary;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rating-summary")
@RequiredArgsConstructor
public class RatingSummaryController {

    private final ProductRatingSummaryRepository summaryRepository;

    @GetMapping("/{productId}")
    public ProductRatingSummaryResponse getSummary(@PathVariable Long productId) {
        com.itc.funkart.model.ProductRatingSummary s = summaryRepository.findById(productId)
                .orElse(new ProductRatingSummary(productId));
        return new ProductRatingSummaryResponse(
                s.getProductId(),
                s.getAverageRating(),
                s.getRatingCount()
        );
    }
}


