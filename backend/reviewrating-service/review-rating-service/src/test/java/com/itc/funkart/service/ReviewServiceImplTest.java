package com.itc.funkart.service;

import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import com.itc.funkart.entity.Review;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import com.itc.funkart.repository.ReviewRepository;
import com.itc.funkart.serviceimpl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRatingSummaryRepository summaryRepository;

    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(reviewRepository, summaryRepository);
    }

    @Test
    void createOrUpdateReview_createsNewWhenNotExists() {
        Long productId = 1L;
        Long userId = 10L;

        when(reviewRepository.findByProductIdAndUserId(productId, userId))
                .thenReturn(Optional.empty());

        Review saved = new Review();
        saved.setId(100L);
        saved.setProductId(productId);
        saved.setUserId(userId);
        saved.setRating(5);
        saved.setComment("Great");

        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ReviewRequest req = new ReviewRequest();
        req.setRating(5);
        req.setComment("Great");

        ReviewResponse resp = reviewService.createOrUpdateReview(productId, userId, req);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review toSave = captor.getValue();

        assertThat(toSave.getProductId()).isEqualTo(productId);
        assertThat(toSave.getUserId()).isEqualTo(userId);
        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getRating()).isEqualTo(5);
    }
}