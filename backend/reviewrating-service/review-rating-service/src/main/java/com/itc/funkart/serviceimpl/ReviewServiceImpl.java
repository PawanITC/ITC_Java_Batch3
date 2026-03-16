package com.itc.funkart.serviceimpl;



import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import com.itc.funkart.entity.ProductRatingSummary;
import com.itc.funkart.entity.Review;
import com.itc.funkart.projection.RatingStats;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import com.itc.funkart.repository.ReviewRepository;
import com.itc.funkart.service.ReviewService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRatingSummaryRepository summaryRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             ProductRatingSummaryRepository summaryRepository) {
        this.reviewRepository = reviewRepository;
        this.summaryRepository = summaryRepository;
    }

    @Override
    @Transactional
    public ReviewResponse createOrUpdateReview(Long productId, Long userId, ReviewRequest request) {
        Review review = reviewRepository
                .findByProductIdAndUserId(productId, userId)
                .orElseGet(() -> {
                    Review r = new Review();
                    r.setProductId(productId);
                    r.setUserId(userId);
                    r.setCreatedAt(Instant.now());
                    return r;
                });

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUpdatedAt(Instant.now());

        Review saved = reviewRepository.save(review);
        recalculateSummary(productId);

        ReviewResponse resp = new ReviewResponse();
        resp.setId(saved.getId());
        resp.setProductId(saved.getProductId());
        resp.setUserId(saved.getUserId());
        resp.setRating(saved.getRating());
        resp.setComment(saved.getComment());
        resp.setCreatedAt(saved.getCreatedAt());
        resp.setUpdatedAt(saved.getUpdatedAt());
        return resp;
    }

    @Override
    @Transactional
    public void deleteReview(Long productId, Long userId) {
        reviewRepository.findByProductIdAndUserId(productId, userId)
                .ifPresent(review -> {
                    reviewRepository.delete(review);
                    recalculateSummary(productId);
                });
    }

    @Override
    public Page<ReviewResponse> getReviewsForProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(r -> {
                    ReviewResponse resp = new ReviewResponse();
                    resp.setId(r.getId());
                    resp.setProductId(r.getProductId());
                    resp.setUserId(r.getUserId());
                    resp.setRating(r.getRating());
                    resp.setComment(r.getComment());
                    resp.setCreatedAt(r.getCreatedAt());
                    resp.setUpdatedAt(r.getUpdatedAt());
                    return resp;
                });
    }

    private void recalculateSummary(Long productId) {
        RatingStats stats = reviewRepository.getRatingStatsByProductId(productId);
        if (stats == null || stats.getAvg() == null) {
            summaryRepository.deleteById(productId);
            return;
        }



        double avg =  stats.getAvg();
        long count = stats.getCount();

        ProductRatingSummary summary = summaryRepository.findById(productId)
                .orElseGet(() -> {
                    ProductRatingSummary s = new ProductRatingSummary();
                    s.setProductId(productId);
                    return s;
                });

        summary.setAverageRating(avg);
        summary.setRatingCount(count);
        summaryRepository.save(summary);
    }
}
