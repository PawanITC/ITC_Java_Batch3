package com.itc.funkart.service;

import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.entity.Review;
import com.itc.funkart.outbox.OutboxService;
import com.itc.funkart.projection.RatingStats;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import com.itc.funkart.repository.ReviewRepository;
import com.itc.funkart.serviceimpl.ReviewServiceImpl;
import org.apache.avro.specific.SpecificRecordBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRatingSummaryRepository summaryRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private final Long productId = 1L;
    private final Long userId = 10L;

    private ReviewRequest request;

    @BeforeEach
    void setUp() {
        request = new ReviewRequest(5, "Great");
    }

    // ⭐ CREATE FLOW
    @Test
    void createOrUpdateReview_createsNewReview() {

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.empty());

        /*when(reviewRepository.getRatingStatsByProductId(productId))
                .thenReturn(mockStats(5.0, 1L));*/

        when(summaryRepository.findById(productId))
                .thenReturn(Optional.empty());

        Review saved = buildReview(100L, 5, "Great");
        when(reviewRepository.save(any())).thenReturn(saved);

        reviewService.createOrUpdateReview(productId, userId, request);

        verify(reviewRepository).save(any());
        verify(summaryRepository).save(any());
        verify(redisTemplate).delete("product:1:rating-summary");

        // ⭐ Outbox event must be saved
        verify(outboxService).saveEventToOutbox(any(SpecificRecordBase.class));
    }

    // ⭐ UPDATE FLOW
    @Test
    void createOrUpdateReview_updatesExistingReview() {

        Review existing = buildReview(100L, 3, "Old");

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.of(existing));

        /*when(reviewRepository.getRatingStatsByProductId(productId))
                .thenReturn(mockStats(4.0, 2L));*/

        when(summaryRepository.findById(productId))
                .thenReturn(Optional.of(new ProductRatingSummary()));

        when(reviewRepository.save(any())).thenReturn(existing);

        reviewService.createOrUpdateReview(productId, userId, request);

        verify(reviewRepository).save(any());
        verify(summaryRepository).save(any());
        verify(redisTemplate).delete("product:1:rating-summary");

        // ⭐ Outbox event must be saved
        verify(outboxService).saveEventToOutbox(any(SpecificRecordBase.class));
    }

    // ⭐ NULL STATS → delete summary
    @Test
    void recalculateSummary_whenStatsNull_shouldDeleteSummary() {

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.empty());

        /*when(reviewRepository.getRatingStatsByProductId(productId))
                .thenReturn(null);*/

        when(reviewRepository.save(any()))
                .thenReturn(buildReview(1L, 5, "Great"));

        reviewService.createOrUpdateReview(productId, userId, request);

        verify(summaryRepository).deleteById(productId);
    }

    // ⭐ EXCEPTION FLOW
    @Test
    void createOrUpdateReview_whenSaveFails_shouldThrowException() {

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.empty());

        /*when(reviewRepository.getRatingStatsByProductId(productId))
                .thenReturn(null);*/

        when(reviewRepository.save(any()))
                .thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() ->
                reviewService.createOrUpdateReview(productId, userId, request)
        ).isInstanceOf(RuntimeException.class);

        verify(outboxService, never()).saveEventToOutbox(any());
        verify(redisTemplate, never()).delete(anyString());
    }

    // ⭐ HELPERS
    private Review buildReview(Long id, int rating, String comment) {

        Review r = new Review(productId, userId, LocalDateTime.now());  // FIXED

        r.setId(id);  // UUID conversion
        r.setRating(rating);
        r.setReviewText(comment);
        r.setUpdatedAt(LocalDateTime.now());

        return r;
    }


    private RatingStats mockStats(Double avg, Long count) {
        RatingStats stats = mock(RatingStats.class);
        when(stats.getAvg()).thenReturn(avg);
        when(stats.getCount()).thenReturn(count);
        return stats;
    }
}
