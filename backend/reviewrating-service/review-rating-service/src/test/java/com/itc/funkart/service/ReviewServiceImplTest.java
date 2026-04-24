
package com.itc.funkart.service;

import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import com.itc.funkart.entity.ProductRatingSummary;
import com.itc.funkart.entity.Review;
import com.itc.funkart.kafka.ReviewEventProducer;
import com.itc.funkart.projection.RatingStats;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import com.itc.funkart.repository.ReviewRepository;
import com.itc.funkart.serviceimpl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRatingSummaryRepository summaryRepository;

    @Mock
    private ReviewEventProducer reviewEventProducer;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private final Long productId = 1L;
    private final Long userId = 10L;

    private ReviewRequest request;

    @BeforeEach
    void setUp() {
        request = new ReviewRequest();
        request.setRating(5);
        request.setComment("Great");
    }

    // ✅ CREATE FLOW
    @Test
    void createOrUpdateReview_createsNewWhenNotExists() {

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.empty());

        when(reviewRepository.getRatingStatsByProductId(productId))
                .thenReturn(mockStats(5.0, 1L));

        when(summaryRepository.findById(productId))
                .thenReturn(Optional.empty());

        Review saved = buildReview(100L, 5, "Great");
        when(reviewRepository.save(any())).thenReturn(saved);

        ReviewResponse resp = reviewService.createOrUpdateReview(productId, userId, request);

        // ✅ verify save
        verify(reviewRepository).save(any());

        // ✅ verify summary updated
        verify(summaryRepository).save(any());

        // ✅ verify kafka
        verify(reviewEventProducer).sendReviewCreatedEvent(any());

        // ✅ verify cache eviction
        verify(redisTemplate).delete("product:1:rating-summary");

        assertThat(resp.getId()).isEqualTo(100L);
    }

    // ✅ UPDATE FLOW
    @Test
    void createOrUpdateReview_updatesExistingReview() {

        Review existing = buildReview(100L, 3, "Old");

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.of(existing));

        when(reviewRepository.getRatingStatsByProductId(productId))
                .thenReturn(mockStats(4.0, 2L));

        when(summaryRepository.findById(productId))
                .thenReturn(Optional.of(new ProductRatingSummary()));

        when(reviewRepository.save(any())).thenReturn(existing);

        reviewService.createOrUpdateReview(productId, userId, request);

        verify(reviewRepository).save(any());
        verify(summaryRepository).save(any());
        verify(reviewEventProducer).sendReviewCreatedEvent(any());
        verify(redisTemplate).delete("product:1:rating-summary");
    }

    // ✅ NULL STATS → delete summary
    @Test
    void recalculateSummary_whenStatsNull_shouldDeleteSummary() {

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.empty());

        when(reviewRepository.getRatingStatsByProductId(productId))
                .thenReturn(null);

        when(reviewRepository.save(any()))
                .thenReturn(buildReview(1L, 5, "Great"));

        reviewService.createOrUpdateReview(productId, userId, request);

        verify(summaryRepository).deleteById(productId);
    }

    // ✅ EXCEPTION FLOW
    @Test
    void createOrUpdateReview_whenSaveFails_shouldThrowException() {

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.empty());

        when(reviewRepository.getRatingStatsByProductId(productId))
                .thenReturn(null);

        when(reviewRepository.save(any()))
                .thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() ->
                reviewService.createOrUpdateReview(productId, userId, request)
        ).isInstanceOf(RuntimeException.class);

        verify(reviewEventProducer, never()).sendReviewCreatedEvent(any());
        verify(redisTemplate, never()).delete(anyString());
    }

    // ✅ HELPER
    private Review buildReview(Long id, int rating, String comment) {
        Review r = new Review();
        r.setId(id);
        r.setProductId(productId);
        r.setUserId(userId);
        r.setRating(rating);
        r.setComment(comment);
        r.setCreatedAt(java.time.Instant.now()); // ✅ ADD THIS
        r.setUpdatedAt(java.time.Instant.now()); // ✅ ADD THIS
        return r;
    }

    private RatingStats mockStats(Double avg, Long count) {
        RatingStats stats = mock(RatingStats.class);
        when(stats.getAvg()).thenReturn(avg);
        when(stats.getCount()).thenReturn(count);
        return stats;
    }
}