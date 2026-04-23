package com.itc.funkart.service;

import com.itc.funkart.event.ReviewCreatedEvent;
import com.itc.funkart.model.RatingSummary;
import com.itc.funkart.model.ReviewProjection;
import com.itc.funkart.repository.ReviewReadRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RatingAggregationService {

    private final Map<String, RatingStats> ratingStore = new ConcurrentHashMap<>();
    private final ReviewReadRepository reviewReadRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public RatingAggregationService(ReviewReadRepository reviewReadRepository,
                                    RedisTemplate<String, Object> redisTemplate) {
        this.reviewReadRepository = reviewReadRepository;
        this.redisTemplate = redisTemplate;
    }


    public void handleNewReview(ReviewCreatedEvent event) {
        ratingStore.compute(event.getProductId(), (productId, stats) -> {
            if (stats == null) {
                stats = new RatingStats();
            }
            stats.addRating(event.getRating());
            return stats;
        });
    }

    public double getAverageRating(String productId) {
        RatingStats stats = ratingStore.get(productId);
        if (stats == null || stats.getCount() == 0) {
            return 0.0;
        }
        return (double) stats.getSum() / stats.getCount();
    }

    private static class RatingStats {
        private int sum;
        private int count;

        void addRating(int rating) {
            this.sum += rating;
            this.count++;
        }

        int getSum() {
            return sum;
        }

        int getCount() {
            return count;
        }
    }

    public RatingSummary recomputeAndCache(String productId) {
        List<ReviewProjection> reviews = reviewReadRepository.findByProductId(productId);

        long total = reviews.size();
        double avg = total == 0 ? 0.0 :
                reviews.stream().mapToInt(ReviewProjection::getRating).average().orElse(0.0);

        Map<Integer, Long> starCounts = reviews.stream()
                .collect(Collectors.groupingBy(
                        ReviewProjection::getRating,
                        Collectors.counting()
                ));

        RatingSummary summary = new RatingSummary(productId, avg, total, starCounts);

        String key = "product:%s:rating-summary".formatted(productId);
        redisTemplate.opsForValue().set(key, summary, Duration.ofMinutes(5));

        return summary;
    }

    public Optional<RatingSummary> getFromCache(String productId) {
        String key = "product:%s:rating-summary".formatted(productId);
        Object value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable((RatingSummary) value);
    }


}

