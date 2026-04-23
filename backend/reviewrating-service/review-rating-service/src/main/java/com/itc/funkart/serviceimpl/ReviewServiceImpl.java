package com.itc.funkart.serviceimpl;



import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import com.itc.funkart.entity.ProductRatingSummary;
import com.itc.funkart.entity.Review;
import com.itc.funkart.event.ReviewCreatedEvent;
import com.itc.funkart.kafka.ReviewEventProducer;
import com.itc.funkart.projection.RatingStats;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import com.itc.funkart.repository.ReviewRepository;
import com.itc.funkart.service.ReviewService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
//import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRatingSummaryRepository summaryRepository;
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("review-service");
    private final ReviewEventProducer reviewEventProducer;
    private final RedisTemplate<String, Object> redisTemplate;



    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             ProductRatingSummaryRepository summaryRepository, ReviewEventProducer reviewEventProducer,RedisTemplate<String, Object> redisTemplate) {
        this.reviewRepository = reviewRepository;
        this.summaryRepository = summaryRepository;
        this.reviewEventProducer = reviewEventProducer;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public ReviewResponse createOrUpdateReview(Long productId, Long userId, ReviewRequest request) {
        Span span = tracer.spanBuilder("createOrUpdateReview").startSpan();
        ReviewResponse resp;

        try {
            span.setAttribute("product.id", productId);
            span.setAttribute("user.id", userId);
            span.setAttribute("rating", request.getRating());

            // 1. Fetch or create review
            Review review = reviewRepository
                    .findByProductIdAndUserId(productId, userId)
                    .orElseGet(() -> {
                        Review r = new Review();
                        r.setProductId(productId);
                        r.setUserId(userId);
                        r.setCreatedAt(Instant.now());
                        return r;
                    });

            // 2. Update fields
            review.setRating(request.getRating());
            review.setComment(request.getComment());
            review.setUpdatedAt(Instant.now());

            // 3. Save to DB
            Review saved = reviewRepository.save(review);

            // 4. Recalculate summary (local sync logic)
            recalculateSummary(productId);

            // 5. Publish Kafka event (NEW)
            ReviewCreatedEvent event = new ReviewCreatedEvent(
                    saved.getId(),
                    saved.getProductId(),
                    saved.getUserId(),
                    saved.getRating(),
                    saved.getComment(),
                    saved.getCreatedAt(),
                    saved.getUpdatedAt()
            );

            reviewEventProducer.sendReviewCreatedEvent(event);

            // 6. Build response
            resp = new ReviewResponse(
                    saved.getId(),
                    saved.getProductId(),
                    saved.getUserId(),
                    saved.getRating(),
                    saved.getComment(),
                    saved.getCreatedAt(),
                    saved.getUpdatedAt()
            );
            // 🔥 Invalidate cache for this product
            String key = "product:%s:rating-summary".formatted(saved.getProductId());
            redisTemplate.delete(key);



        } finally {
            span.end();
        }

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

    public ReviewCreatedEvent createReview(ReviewRequest request) {
        // In real system: persist to DB first, then publish event
        ReviewCreatedEvent event = new ReviewCreatedEvent(request.getId(),
                Long.valueOf(request.getProductId()),
                Long.valueOf(request.getUserId()),
                request.getRating(),
                request.getComment(),
                Instant.now(),
                Instant.now()
        );

        reviewEventProducer.sendReviewCreatedEvent(event);
        return event;
    }
}
