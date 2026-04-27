package com.itc.funkart.serviceimpl;



import com.itc.funkart.model.ProductRatingSummary;
import com.itc.funkart.model.RatingStats;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import com.itc.funkart.repository.ReviewReadModelRepository;
import com.itc.funkart.service.RatingAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingAggregationServiceImpl implements RatingAggregationService {

    private final ReviewReadModelRepository reviewReadModelRepository;
    private final ProductRatingSummaryRepository summaryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY = "product:%s:rating-summary";


    @Override
    public void handleCreated(Long productId, int rating) {
        recalcAndCache(productId);
    }

    @Override
    public void handleUpdated(Long productId) {
        recalcAndCache(productId);
    }

    @Override
    public void handleDeleted(Long productId) {
        recalcAndCache(productId);
    }

    private void recalcAndCache(Long productId) {

        RatingStats stats = reviewReadModelRepository.getRatingStatsByProductId(productId);

        if (stats == null || stats.getCount() == null || stats.getCount() == 0 || stats.getAvg() == null) {
            summaryRepository.deleteById(productId);
            redisTemplate.delete(KEY.formatted(productId));
            return;
        }

        ProductRatingSummary summary = summaryRepository.findById(productId)
                .orElseGet(() -> new ProductRatingSummary(productId));

        summary.setAverageRating(stats.getAvg());
        summary.setRatingCount(stats.getCount());

        summaryRepository.save(summary);

        redisTemplate.opsForValue().set(
                KEY.formatted(productId),
                summary,
                Duration.ofMinutes(5)
        );

        log.info("Updated rating summary for product {} -> avg={}, count={}",
                productId, stats.getAvg(), stats.getCount());
    }

    @Override
    public Optional<ProductRatingSummary> getFromCache(Long productId) {
        String key = "product:%s:rating-summary".formatted(productId);
        Object cached = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable((ProductRatingSummary) cached);
    }

    @Override
    public ProductRatingSummary recomputeAndCache(Long productId) {
        recalcAndCache(productId);
        return summaryRepository.findById(productId).orElse(null);
    }
}

