package com.itc.funkart.review.service;

import com.itc.funkart.common.dto.security.UserPrincipalDto;
import com.itc.funkart.review.dto.request.ReviewRequest;
import com.itc.funkart.review.dto.response.ReviewResponse;
import com.itc.funkart.review.entity.Review;
import com.itc.funkart.review.exception.ForbiddenException;
import com.itc.funkart.review.exception.NotFoundException;
import com.itc.funkart.review.kafka.ReviewEventPublisher;
import com.itc.funkart.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewEventPublisher eventPublisher;

    /**
     * Returns a page of reviews for a product — public, no auth required.
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsForProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(ReviewResponse::from);
    }

    /**
     * Creates a review. One review per user per product is enforced.
     */
    @Transactional
    public ReviewResponse createReview(Long productId, ReviewRequest request, UserPrincipalDto principal) {
        if (reviewRepository.existsByProductIdAndUserId(productId, principal.userId())) {
            throw new ForbiddenException("You have already reviewed this product");
        }

        Review review = Review.builder()
                .productId(productId)
                .userId(principal.userId())
                .author(principal.name())
                .title(request.getTitle())
                .comment(request.getComment())
                .rating(request.getRating())
                .likes(0)
                .build();

        review = reviewRepository.save(review);

        eventPublisher.publishReviewCreated(review.getId(), productId, principal.userId(), review.getRating());

        log.info("Review {} created for product {} by user {}", review.getId(), productId, principal.userId());
        return ReviewResponse.from(review);
    }

    /**
     * Moderator/admin hard delete. No ownership check — any moderator can delete any review.
     */
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found: " + reviewId));

        reviewRepository.delete(review);
        eventPublisher.publishReviewDeleted(review.getId(), review.getProductId(), review.getUserId(), review.getRating());

        log.info("Review {} deleted (moderator action)", reviewId);
    }
}
