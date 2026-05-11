package com.itc.funkart.review.controller;

import com.itc.funkart.common.dto.security.UserPrincipalDto;
import com.itc.funkart.review.dto.request.ReviewRequest;
import com.itc.funkart.review.dto.response.ReviewResponse;
import com.itc.funkart.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Review controller — paths are relative after StripPrefix=2.
 *
 * <p>Gateway routes {@code /api/v1/reviews/**} here as {@code /reviews/**}.
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * GET /api/v1/reviews/{productId}?page=0&size=20
     * Public — no auth required.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ReviewResponse> reviews = reviewService.getReviewsForProduct(
                productId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ResponseEntity.ok(reviews);
    }

    /**
     * POST /api/v1/reviews/{productId}
     * Authenticated users only. One review per user per product.
     */
    @PostMapping("/{productId}")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {

        UserPrincipalDto principal = (UserPrincipalDto) authentication.getPrincipal();
        ReviewResponse created = reviewService.createReview(productId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * DELETE /api/v1/reviews/admin/{reviewId}
     * ADMIN or MODERATOR only — secured at SecurityConfig level.
     */
    @DeleteMapping("/admin/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
