package com.itc.funkart.service;

import com.itc.funkart.dto.RatingStatsDto;
import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.entity.ProductRatingSummary;
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

    // ---------------------------------------------------------
    // ⭐ CREATE FLOW
    // ---------------------------------------------------------
    @Test
    void createReview_whenNoExistingReview_shouldCreateNewReview() {

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.empty());

        when(summaryRepository.findById(productId))
                .thenReturn(Optional.empty());

        when(reviewRepository.save(any()))
                .thenReturn(buildReview(100L, 5, "Great"));

        reviewService.createOrUpdateReview(productId, userId, request);

        verify(reviewRepository).save(any());
        verify(summaryRepository).save(any());
        verify(redisTemplate).delete("product:1:rating-summary");
        verify(outboxService).saveEventToOutbox(any(SpecificRecordBase.class));
    }

    // ---------------------------------------------------------
    // ⭐ UPDATE FLOW
    // ---------------------------------------------------------
    @Test
    void updateReview_whenExistingReview_shouldUpdate() {

        Review existing = buildReview(100L, 3, "Old");

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.of(existing));

        when(summaryRepository.findById(productId))
                .thenReturn(Optional.of(new ProductRatingSummary()));

        when(reviewRepository.save(any()))
                .thenReturn(existing);

        reviewService.createOrUpdateReview(productId, userId, request);

        verify(reviewRepository).save(any());
        verify(summaryRepository).save(any());
        verify(redisTemplate).delete("product:1:rating-summary");
        verify(outboxService).saveEventToOutbox(any(SpecificRecordBase.class));
    }

    // ---------------------------------------------------------
    // ⭐ SUMMARY UPDATE FLOW (stats non-null)
    // ---------------------------------------------------------
    @Test
    void recalcSummary_whenStatsNotNull_shouldUpdateSummary() {

        Review existing = buildReview(100L, 4, "Nice");

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.of(existing));

        RatingStatsDto stats = new RatingStatsDto(4.5, 10L);

        when(reviewRepository.getRatingStatsByProductId(productId))
                .thenReturn(stats);


        when(summaryRepository.findById(productId))
                .thenReturn(Optional.of(new ProductRatingSummary()));

        when(reviewRepository.save(any()))
                .thenReturn(existing);

        reviewService.createOrUpdateReview(productId, userId, request);

        verify(summaryRepository).save(any());
    }

    // ---------------------------------------------------------
    // ⭐ SUMMARY DELETE FLOW (stats null)
    // ---------------------------------------------------------
    @Test
    void recalcSummary_whenStatsNull_shouldDeleteSummary() {

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.empty());

        when(reviewRepository.getRatingStatsByProductId(productId))
                .thenReturn(null);

        when(reviewRepository.save(any()))
                .thenReturn(buildReview(1L, 5, "Great"));

        reviewService.createOrUpdateReview(productId, userId, request);

        verify(summaryRepository).deleteById(productId);
    }

    // ---------------------------------------------------------
    // ⭐ DELETE REVIEW FLOW
    // ---------------------------------------------------------
    @Test
    void deleteReview_shouldDeleteAndSendEvent() {

        Review existing = buildReview(100L, 5, "Good");

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.of(existing));

        reviewService.deleteReview(productId, userId);

        verify(reviewRepository).delete(existing);
        verify(redisTemplate).delete("product:1:rating-summary");
        verify(outboxService).saveEventToOutbox(any(SpecificRecordBase.class));
    }

    // ---------------------------------------------------------
    // ⭐ EXCEPTION FLOW
    // ---------------------------------------------------------
    @Test
    void createReview_whenSaveFails_shouldThrowException() {

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.empty());

        when(reviewRepository.save(any()))
                .thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() ->
                reviewService.createOrUpdateReview(productId, userId, request)
        ).isInstanceOf(RuntimeException.class);

        verify(outboxService, never()).saveEventToOutbox(any());
        verify(redisTemplate, never()).delete(anyString());
    }

    // ---------------------------------------------------------
    // ⭐ HELPERS
    // ---------------------------------------------------------
    private Review buildReview(Long id, int rating, String comment) {
        Review r = new Review(productId, userId, LocalDateTime.now());
        r.setId(id);
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