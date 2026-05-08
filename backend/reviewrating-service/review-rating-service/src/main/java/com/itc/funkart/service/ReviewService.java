package com.itc.funkart.service;

import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createOrUpdateReview(Long productId, Long userId, ReviewRequest request);

    Page<ReviewResponse> getReviewsForProduct(Long productId, Pageable pageable);

    void deleteReview(Long productId, Long userId);

    void adminDeleteReview(Long reviewId);
}
