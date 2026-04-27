package com.itc.funkart.serviceimpl;

import com.example.events.common.ReviewPayload;
import com.itc.funkart.avro.ReviewCreatedEvent;
import com.itc.funkart.avro.ReviewDeletedEvent;
import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import com.itc.funkart.entity.Review;
import com.itc.funkart.avro.ReviewDeletedEvent;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OutboxService outboxService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public ReviewResponse createOrUpdateReview(Long productId, Long userId, ReviewRequest request) {

        Review review = reviewRepository
                .findByProductIdAndUserId(productId, userId)
                .orElseGet(() -> new Review(productId, userId, Instant.now()));

        boolean isNew = review.getId() == null;

        review.setRating(request.getRating());
        review.setReviewText(request.getComment());
        review.setUpdatedAt(Instant.now());

        Review saved = reviewRepository.save(review);

        redisTemplate.delete("product:%s:rating-summary".formatted(productId));

        ReviewPayload payload = ReviewPayload.newBuilder()
                .setReviewId(saved.getId().toString())
                .setProductId(Long.valueOf(saved.getProductId()))
                .setUserId(Long.valueOf(saved.getUserId()))
                .setRating(saved.getRating())
                .setComment(saved.getReviewText())
                .setCreatedAt(saved.getCreatedAt())
                .build();

        if (isNew) {
            ReviewCreatedEvent event = ReviewCreatedEvent.newBuilder()
                    .setEventType("REVIEW_CREATED")
                    .setPayload(payload)
                    .build();

            outboxService.saveEventToOutbox(event);

        } else {
            ReviewUpdatedEvent event = ReviewUpdatedEvent.newBuilder()
                    .setEventType("REVIEW_UPDATED")
                    .setPayload(payload)
                    .build();

            outboxService.saveEventToOutbox(event);
        }

        return ReviewResponse.from(saved);
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
                            .setPayload(
                                    ReviewPayload.newBuilder()
                                            .setReviewId(review.getId().toString())
                                            .setProductId(Long.valueOf(review.getProductId()))
                                            .setUserId(Long.valueOf(review.getUserId()))
                                            .setRating(review.getRating())
                                            .setComment(review.getReviewText())
                                            .setCreatedAt(review.getCreatedAt())
                                            .build()
                            )
                            .build();

                    outboxService.saveEventToOutbox(event);
                });
    }

    @Override
    public Page<ReviewResponse> getReviewsForProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(r -> new ReviewResponse(
                        r.getId(),
                        Long.valueOf(r.getProductId()),
                        Long.valueOf(r.getUserId()),
                        r.getRating(),
                        r.getReviewText(),
                        r.getCreatedAt(),
                        r.getUpdatedAt()
                ));
    }
}
