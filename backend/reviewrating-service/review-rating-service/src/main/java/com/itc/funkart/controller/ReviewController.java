package com.itc.funkart.controller;

import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import com.itc.funkart.entity.Review;
import com.itc.funkart.repository.ReviewRepository;
import com.itc.funkart.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewRepository repo;
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService,ReviewRepository repo) {
        this.reviewService = reviewService;
        this.repo = repo;
    }

    /*public ReviewController(ReviewRepository repo) {
        this.repo = repo;
    }*/

    @PostMapping("/{productId}")
    public ReviewResponse createOrUpdateReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {

        Long userId = Long.valueOf(authentication.getName());
        return reviewService.createOrUpdateReview(productId, userId, request);
    }


    @GetMapping("/{productId}/{userId}")
    public Review get(
            @PathVariable Long productId,
            @PathVariable Long userId) {
        return repo.findByProductIdAndUserId(productId, userId)
                .orElseThrow(() -> new RuntimeException("Not found"));
    }

    @GetMapping("/{productId}")
    public Page<ReviewResponse> getReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getReviewsForProduct(productId, pageable);
    }

    @DeleteMapping("/{productId}")
    public void deleteReview(
            @PathVariable Long productId,
            Authentication authentication) {

        Long userId = Long.valueOf(authentication.getName());
        reviewService.deleteReview(productId, userId);
    }
}

