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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// NOTE: path is /reviews (no /api/v1 prefix) because the API Gateway strips
// the first two segments (/api/v1) via StripPrefix=2 before forwarding here.
@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewRepository repo;
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService, ReviewRepository repo) {
        this.reviewService = reviewService;
        this.repo = repo;
    }

    // POST /api/v1/reviews/{productId} — create or update the authenticated user's review
    @PostMapping("/{productId}")
    public ReviewResponse createOrUpdateReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {

        Long userId = Long.valueOf(authentication.getName());
        return reviewService.createOrUpdateReview(productId, userId, request);
    }

    // GET /api/v1/reviews/{productId}/{userId} — fetch a specific user's review
    @GetMapping("/{productId}/{userId}")
    public Review get(
            @PathVariable Long productId,
            @PathVariable Long userId) {
        return repo.findByProductIdAndUserId(productId, userId)
                .orElseThrow(() -> new RuntimeException("Not found"));
    }

    // GET /api/v1/reviews/{productId}?page=0&size=10 — paginated review list
    @GetMapping("/{productId}")
    public Page<ReviewResponse> getReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getReviewsForProduct(productId, pageable);
    }

    // DELETE /api/v1/reviews/{productId} — owner deletes their own review
    @DeleteMapping("/{productId}")
    public void deleteReview(
            @PathVariable Long productId,
            Authentication authentication) {

        Long userId = Long.valueOf(authentication.getName());
        reviewService.deleteReview(productId, userId);
    }

    // DELETE /api/v1/reviews/admin/{reviewId} — ROLE_ADMIN or ROLE_MODERATOR
    // deletes any review by its ID (for moderation purposes)
    @DeleteMapping("/admin/{reviewId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Void> moderatorDeleteReview(@PathVariable Long reviewId) {
        repo.findById(reviewId).ifPresent(review -> {
            repo.deleteById(reviewId);
        });
        return ResponseEntity.noContent().build();
    }
}

