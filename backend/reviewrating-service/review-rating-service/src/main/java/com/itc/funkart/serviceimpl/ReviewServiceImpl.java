package com.itc.funkart.serviceimpl;

import com.example.events.common.ReviewPayload;
import com.itc.funkart.avro.ReviewCreatedEvent;
import com.itc.funkart.avro.ReviewDeletedEvent;
import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import com.itc.funkart.entity.Review;
import com.itc.funkart.event.ReviewUpdatedEvent;
import com.itc.funkart.outbox.OutboxService;
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
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    //private final ReviewRepository reviewRepository;
    private final OutboxService outboxService;
    private final RedisTemplate<String, Object> redisTemplate;
    public ReviewServiceImpl(ReviewRepository reviewRepository,OutboxService outboxService,RedisTemplate<String, Object> redisTemplate) {
        this.reviewRepository = reviewRepository;
        this.redisTemplate= redisTemplate;
        this.outboxService =outboxService;
    }


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
        review.setUpdatedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);

        Instant createdAtInstant = saved.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .toInstant();

        Instant updatedAtInstant = saved.getUpdatedAt()
                .atZone(ZoneId.systemDefault())
                .toInstant();

        redisTemplate.delete("product:%s:rating-summary".formatted(productId));

        ReviewPayload payload = ReviewPayload.newBuilder()
                .setReviewId(Long.valueOf(saved.getId().toString()))
                .setProductId(Long.valueOf(saved.getProductId()))
                .setUserId(Long.valueOf(saved.getUserId()))
                .setRating(saved.getRating())
                .setComment(saved.getReviewText())
                .setCreatedAt(
                        createdAtInstant
                )
                .build();

        if (isNew) {
            ReviewCreatedEvent event = ReviewCreatedEvent.newBuilder()
                    .setEventType("REVIEW_CREATED")
                    .setReviewId(saved.getId().toString())
                    .setProductId(saved.getProductId())
                    .setUserId(saved.getUserId())
                    .setRating(saved.getRating())
                    .setComment(saved.getReviewText())
                    .setCreatedAt(createdAtInstant)
                    .setUpdatedAt(updatedAtInstant)
                    .setPayload(payload)
                    .build();

            outboxService.saveEventToOutbox(event);

        } else {
            ReviewUpdatedEvent event = ReviewUpdatedEvent.newBuilder()
                    .setEventType("REVIEW_UPDATED")
                    .setReviewId(saved.getId().toString())
                    .setProductId(saved.getProductId())
                    .setUserId(saved.getUserId())
                    .setRating(saved.getRating())
                    .setComment(saved.getReviewText())
                    .setCreatedAt(createdAtInstant)
                    .setUpdatedAt(updatedAtInstant)
                    .setPayload(payload)
                    .build();

            outboxService.saveEventToOutbox(event);
        }

        return ReviewResponse.from(saved);
    }


    @Override
    public Review createReview(Review review) {
        review.setCreatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    @Override
    public Review getReview(Long productId, Long userId) {
        return reviewRepository.findByProductIdAndUserId(productId, userId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
    }

    @Override
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

                    redisTemplate.delete("product:%s:rating-summary".formatted(productId));

                    ReviewDeletedEvent event = ReviewDeletedEvent.newBuilder()
                            .setEventType("REVIEW_DELETED")
                            .setReviewId(review.getId().toString())              // ✅ FIX
                            .setProductId(review.getProductId())           // ✅ REQUIRED
                            .setUserId(review.getUserId())                 // ✅ REQUIRED
                            .setRating(review.getRating())                 // ✅ REQUIRED
                            .setComment(review.getReviewText())            // ✅ REQUIRED
                            .setCreatedAt(review.getCreatedAt().atZone(ZoneId.systemDefault())
                                    .toInstant()) // ✅ FIX type
                            .setDeletedAt(Instant.now())
                            .setPayload(
                                    ReviewPayload.newBuilder()
                                            .setReviewId(Long.valueOf(review.getId().toString()))
                                            .setProductId(Long.valueOf(review.getProductId()))
                                            .setUserId(Long.valueOf(review.getUserId()))
                                            .setRating(review.getRating())
                                            .setComment(review.getReviewText())
                                            .setCreatedAt(review.getCreatedAt().atZone(ZoneId.systemDefault())
                                            .toInstant())
                                            .build())
                            .build();

                    outboxService.saveEventToOutbox(event);
                });
    }


}
