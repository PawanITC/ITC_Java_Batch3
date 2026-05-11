package com.itc.funkart.review.service;

import com.itc.funkart.common.dto.security.UserPrincipalDto;
import com.itc.funkart.review.dto.request.ReviewRequest;
import com.itc.funkart.review.dto.response.ReviewResponse;
import com.itc.funkart.review.entity.Review;
import com.itc.funkart.review.exception.ForbiddenException;
import com.itc.funkart.review.exception.NotFoundException;
import com.itc.funkart.review.kafka.ReviewEventPublisher;
import com.itc.funkart.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    ReviewRepository reviewRepository;
    @Mock
    ReviewEventPublisher eventPublisher;
    @InjectMocks
    ReviewService reviewService;

    private UserPrincipalDto principal;
    private Review sampleReview;

    @BeforeEach
    void setUp() {
        principal = UserPrincipalDto.builder()
                .userId(1L)
                .name("Test User")
                .email("test@example.com")
                .role("ROLE_USER")
                .build();

        sampleReview = Review.builder()
                .id(10L)
                .productId(5L)
                .userId(1L)
                .author("Test User")
                .title("Great product")
                .comment("Really enjoyed it")
                .rating(5)
                .likes(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─── getReviewsForProduct ─────────────────────────────────────────────────

    @Test
    void getReviewsForProduct_returnsPagedResults() {
        Page<Review> page = new PageImpl<>(List.of(sampleReview));
        when(reviewRepository.findByProductId(eq(5L), any())).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getReviewsForProduct(5L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRating()).isEqualTo(5);
        assertThat(result.getContent().get(0).getAuthor()).isEqualTo("Test User");
    }

    @Test
    void getReviewsForProduct_noReviews_returnsEmptyPage() {
        when(reviewRepository.findByProductId(eq(99L), any())).thenReturn(Page.empty());

        Page<ReviewResponse> result = reviewService.getReviewsForProduct(99L, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    // ─── createReview ─────────────────────────────────────────────────────────

    @Test
    void createReview_happyPath_savesAndPublishesEvent() {
        ReviewRequest request = new ReviewRequest();
        request.setTitle("Great product");
        request.setComment("Really enjoyed it");
        request.setRating(5);

        when(reviewRepository.existsByProductIdAndUserId(5L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(sampleReview);

        ReviewResponse result = reviewService.createReview(5L, request, principal);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getRating()).isEqualTo(5);
        verify(reviewRepository).save(any(Review.class));
        verify(eventPublisher).publishReviewCreated(eq(10L), eq(5L), eq(1L), eq(5));
    }

    @Test
    void createReview_duplicateReview_throwsForbiddenException() {
        ReviewRequest request = new ReviewRequest();
        request.setTitle("Again");
        request.setComment("Second review attempt");
        request.setRating(3);

        when(reviewRepository.existsByProductIdAndUserId(5L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(5L, request, principal))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("already reviewed");

        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // ─── deleteReview ─────────────────────────────────────────────────────────

    @Test
    void deleteReview_existingReview_deletesAndPublishesEvent() {
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(sampleReview));

        reviewService.deleteReview(10L);

        verify(reviewRepository).delete(sampleReview);
        verify(eventPublisher).publishReviewDeleted(eq(10L), eq(5L), eq(1L), eq(5));
    }

    @Test
    void deleteReview_notFound_throwsNotFoundException() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");

        verify(reviewRepository, never()).delete(any());
        verifyNoInteractions(eventPublisher);
    }
}
