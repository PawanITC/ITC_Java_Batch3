package com.itc.funkart.controller;

import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import com.itc.funkart.service.ReviewService;
import com.itc.funkart.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// NOTE: path is /reviews — API Gateway strips /api/v1 via StripPrefix=2
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // POST /api/v1/reviews/{productId} — create or update the caller's review
    @PostMapping("/{productId}")
    public ReviewResponse createOrUpdateReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request) {

        Long userId = SecurityUtils.getCurrentUserId();
        return reviewService.createOrUpdateReview(productId, userId, request);
    }

    // GET /api/v1/reviews/{productId}?page=0&size=10 — paginated review list (public)
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
    public ResponseEntity<Void> deleteReview(@PathVariable Long productId) {
        Long userId = SecurityUtils.getCurrentUserId();
        reviewService.deleteReview(productId, userId);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/v1/reviews/admin/{reviewId} — ADMIN or MODERATOR soft-removes any review
    @DeleteMapping("/admin/{reviewId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<Void> adminDeleteReview(@PathVariable Long reviewId) {
        reviewService.adminDeleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
