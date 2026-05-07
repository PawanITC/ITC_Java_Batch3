package com.itc.funkart.serviceimpl;



import com.itc.funkart.model.ProductRatingSummary;
import com.itc.funkart.model.RatingStats;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import com.itc.funkart.repository.ReviewReadModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RatingAggregationServiceImplTest {

    private ReviewReadModelRepository reviewRepo;
    private ProductRatingSummaryRepository summaryRepo;
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOps;

    private RatingAggregationServiceImpl service;

    @BeforeEach
    void setup() {
        reviewRepo = mock(ReviewReadModelRepository.class);
        summaryRepo = mock(ProductRatingSummaryRepository.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        service = new RatingAggregationServiceImpl(reviewRepo, summaryRepo, redisTemplate);
    }

    @Test
    void testRecalcAndCache_statsNull_deletesSummaryAndCache() {
        when(reviewRepo.getRatingStatsByProductId(1L)).thenReturn(null);

        service.handleCreated(1L, 5);

        verify(summaryRepo).deleteById(1L);
        verify(redisTemplate).delete("product:1:rating-summary");
    }

    @Test
    void testRecalcAndCache_statsZeroCount_deletesSummaryAndCache() {
        RatingStats stats = new RatingStats(Double.valueOf(0L), null);
        when(reviewRepo.getRatingStatsByProductId(1L)).thenReturn(stats);

        service.handleUpdated(1L);

        verify(summaryRepo).deleteById(1L);
        verify(redisTemplate).delete("product:1:rating-summary");
    }

    @Test
    void testRecalcAndCache_validStats_savesAndCaches() {
        // avg = 4.2, count = 10
        RatingStats stats = new RatingStats(4.2, 10L);
        when(reviewRepo.getRatingStatsByProductId(1L)).thenReturn(stats);

        ProductRatingSummary summary = new ProductRatingSummary(1L);
        when(summaryRepo.findById(1L)).thenReturn(Optional.of(summary));

        service.handleCreated(1L, 5);

        verify(summaryRepo).save(summary);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(keyCaptor.capture(), eq(summary), eq(Duration.ofMinutes(5)));

        assertEquals("product:1:rating-summary", keyCaptor.getValue());
    }

    @Test
    void testGetFromCache_hit() {
        when(valueOps.get("product:1:rating-summary")).thenReturn(new ProductRatingSummary(1L));

        Optional<ProductRatingSummary> result = service.getFromCache(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getProductId());
    }

    @Test
    void testGetFromCache_miss() {
        when(valueOps.get("product:1:rating-summary")).thenReturn(null);

        Optional<ProductRatingSummary> result = service.getFromCache(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testRecomputeAndCache() {
        ProductRatingSummary summary = new ProductRatingSummary(1L);

        when(summaryRepo.findById(1L)).thenReturn(Optional.of(summary));

        ProductRatingSummary result = service.recomputeAndCache(1L);

        assertEquals(summary, result);
    }
}

