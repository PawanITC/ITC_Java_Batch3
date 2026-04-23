package com.itc.funkart.service;

import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import com.itc.funkart.entity.ProductRatingSummary;
import com.itc.funkart.entity.Review;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import com.itc.funkart.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public interface ReviewService {

    ReviewResponse createOrUpdateReview(Long productId, Long userId, ReviewRequest request);

    void deleteReview(Long productId, Long userId);

    Page<ReviewResponse> getReviewsForProduct(Long productId, Pageable pageable);
}