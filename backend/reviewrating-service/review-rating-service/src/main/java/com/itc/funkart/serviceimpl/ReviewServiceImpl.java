package com.itc.funkart.serviceimpl;

import com.itc.funkart.dto.RatingStatsDto;
import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import com.itc.funkart.entity.ProductRatingSummary;
import com.itc.funkart.entity.Review;
import com.itc.funkart.event.ReviewCreatedEvent;
import com.itc.funkart.event.ReviewDeletedEvent;
import com.itc.funkart.event.ReviewUpdatedEvent;
import com.itc.funkart.kafka.producer.ReviewEventProducer;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import com.itc.funkart.repository.ReviewRepository;
import com.itc.funkart.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRatingSummaryRepository summaryRepository;
    private final ReviewEventProducer reviewEventProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RATING_CACHE_KEY = "product:%s:rating-summary";

    @Override
    @Transactional
    public ReviewResponse createOrUpdateReview(Long productId, Long userId, ReviewRequest request) {

        Review review = reviewRepository
                .findByProductIdAndUserId(productId, userId)
                .orElseGet(() -> new Review(productId, userId, LocalDateTime.now()));

        boolean isNew = review.getId() == null;

        review.setRating(request.getRating());
        review.setReviewText(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            review.setCreatedAt(LocalDateTime.now());
        }

        Review saved = reviewRepository.save(review);
        recalculateSummary(productId);
        evictCache(productId);

        Instant createdAt = saved.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
        Instant updatedAt = saved.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant();

        Object event = isNew
                ? ReviewCreatedEvent.of(saved.getId(), productId, userId, saved.getRating(), saved.getReviewText(), createdAt, updatedAt)
                : ReviewUpdatedEvent.of(saved.getId(), productId, userId, saved.getRating(), saved.getReviewText(), createdAt, updatedAt);

        reviewEventProducer.send(productId.toString(), event);
        log.debug("{} review id={} productId={} userId={}", isNew ? "Created" : "Updated", saved.getId(), productId, userId);

        return ReviewResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsForProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(ReviewResponse::from);
    }

    @Override
    @Transactional
    public void deleteReview(Long productId, Long userId) {
        reviewRepository.findByProductIdAndUserId(productId, userId)
                .ifPresent(review -> {
                    reviewRepository.delete(review);
                    recalculateSummary(productId);
                    evictCache(productId);

                    ReviewDeletedEvent event = ReviewDeletedEvent.of(
                            review.getId(), productId, userId, review.getRating());
                    reviewEventProducer.send(productId.toString(), event);
                    log.debug("Deleted review id={} productId={} userId={}", review.getId(), productId, userId);
                });
    }

    @Override
    @Transactional
    public void adminDeleteReview(Long reviewId) {
        reviewRepository.findById(reviewId).ifPresent(review -> {
            Long productId = review.getProductId();
            reviewRepository.delete(review);
            recalculateSummary(productId);
            evictCache(productId);

            ReviewDeletedEvent event = ReviewDeletedEvent.of(
                    review.getId(), productId, review.getUserId(), review.getRating());
            reviewEventProducer.send(productId.toString(), event);
            log.debug("Admin deleted review id={} productId={}", reviewId, productId);
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void recalculateSummary(Long productId) {
        RatingStatsDto stats = reviewRepository.getRatingStatsByProductId(productId);

        if (stats == null || stats.getAvg() == null) {
            summaryRepository.deleteById(productId);
            return;
        }

        ProductRatingSummary summary = summaryRepository.findById(productId)
                .orElseGet(() -> new ProductRatingSummary(productId));

        summary.setAverageRating(stats.getAvg());
        summary.setRatingCount(stats.getCount());
        summaryRepository.save(summary);
    }

    private void evictCache(Long productId) {
        redisTemplate.delete(RATING_CACHE_KEY.formatted(productId));
    }
}
