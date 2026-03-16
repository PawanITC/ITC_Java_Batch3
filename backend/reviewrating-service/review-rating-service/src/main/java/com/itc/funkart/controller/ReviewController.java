package com.itc.funkart.controller;

import com.itc.funkart.dto.ProductRatingSummaryResponse;
import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import com.itc.funkart.entity.ProductRatingSummary;
import com.itc.funkart.entity.Review;
import com.itc.funkart.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // In real app, userId comes from JWT; here we fake it
    private Long getCurrentUserId() {
        return 1L;
    }

    @PostMapping("/{productId}")
    public ReviewResponse createOrUpdateReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request) {
        return reviewService.createOrUpdateReview(productId, getCurrentUserId(), request);
    }

    @DeleteMapping("/{productId}")
    public void deleteReview(@PathVariable Long productId) {
        reviewService.deleteReview(productId, getCurrentUserId());
    }

    @GetMapping("/{productId}")
    public Page<ReviewResponse> getReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getReviewsForProduct(productId, pageable);
    }

}
