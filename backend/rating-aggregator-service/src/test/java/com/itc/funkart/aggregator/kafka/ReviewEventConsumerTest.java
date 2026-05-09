package com.itc.funkart.aggregator.kafka;

import com.itc.funkart.aggregator.entity.RatingSummary;
import com.itc.funkart.aggregator.repository.RatingSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewEventConsumerTest {

    @Mock RatingSummaryRepository summaryRepository;
    @InjectMocks ReviewEventConsumer consumer;

    // ─── REVIEW_CREATED — no existing summary ────────────────────────────────

    @Test
    void onReviewEvent_created_noExistingSummary_createsNewRow() {
        when(summaryRepository.findById(5L)).thenReturn(Optional.empty());
        when(summaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.onReviewEvent(Map.of(
                "eventType", "REVIEW_CREATED",
                "productId", 5,
                "rating", 4
        ), "review.events.v1");

        ArgumentCaptor<RatingSummary> captor = ArgumentCaptor.forClass(RatingSummary.class);
        verify(summaryRepository).save(captor.capture());

        RatingSummary saved = captor.getValue();
        assertThat(saved.getTotalReviews()).isEqualTo(1);
        assertThat(saved.getFourStar()).isEqualTo(1);
        assertThat(saved.getAverageRating()).isEqualTo(4.0);
    }

    // ─── REVIEW_CREATED — existing summary ───────────────────────────────────

    @Test
    void onReviewEvent_created_existingSummary_incrementsAndRecalculates() {
        RatingSummary existing = RatingSummary.builder()
                .productId(5L).totalReviews(2).sumRatings(8)
                .oneStar(0).twoStar(0).threeStar(0).fourStar(2).fiveStar(0)
                .averageRating(4.0).build();

        when(summaryRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(summaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.onReviewEvent(Map.of(
                "eventType", "REVIEW_CREATED",
                "productId", 5,
                "rating", 5
        ), "review.events.v1");

        ArgumentCaptor<RatingSummary> captor = ArgumentCaptor.forClass(RatingSummary.class);
        verify(summaryRepository).save(captor.capture());

        RatingSummary saved = captor.getValue();
        assertThat(saved.getTotalReviews()).isEqualTo(3);
        assertThat(saved.getFiveStar()).isEqualTo(1);
        assertThat(saved.getAverageRating()).isCloseTo(4.33, org.assertj.core.data.Offset.offset(0.01));
    }

    // ─── REVIEW_DELETED ───────────────────────────────────────────────────────

    @Test
    void onReviewEvent_deleted_decrementsAndRecalculates() {
        RatingSummary existing = RatingSummary.builder()
                .productId(5L).totalReviews(3).sumRatings(13)
                .oneStar(0).twoStar(0).threeStar(0).fourStar(2).fiveStar(1)
                .averageRating(4.33).build();

        when(summaryRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(summaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.onReviewEvent(Map.of(
                "eventType", "REVIEW_DELETED",
                "productId", 5,
                "rating", 5
        ), "review.events.v1");

        ArgumentCaptor<RatingSummary> captor = ArgumentCaptor.forClass(RatingSummary.class);
        verify(summaryRepository).save(captor.capture());

        RatingSummary saved = captor.getValue();
        assertThat(saved.getTotalReviews()).isEqualTo(2);
        assertThat(saved.getFiveStar()).isEqualTo(0);
        assertThat(saved.getAverageRating()).isEqualTo(4.0);
    }

    // ─── Unknown / malformed events ───────────────────────────────────────────

    @Test
    void onReviewEvent_unknownType_skipsAndDoesNotSave() {
        consumer.onReviewEvent(Map.of(
                "eventType", "SOME_FUTURE_EVENT",
                "productId", 5,
                "rating", 3
        ), "review.events.v1");

        verify(summaryRepository, never()).save(any());
    }

    @Test
    void onReviewEvent_missingProductId_skipsWithWarning() {
        consumer.onReviewEvent(Map.of(
                "eventType", "REVIEW_CREATED",
                "rating", 3
        ), "review.events.v1");

        verify(summaryRepository, never()).save(any());
    }

    @Test
    void onReviewEvent_repositoryThrows_exceptionIsSwallowed() {
        when(summaryRepository.findById(any())).thenThrow(new RuntimeException("DB down"));

        // Must not propagate — bad events should be swallowed so consumer offset advances
        consumer.onReviewEvent(Map.of(
                "eventType", "REVIEW_CREATED",
                "productId", 5,
                "rating", 4
        ), "review.events.v1");

        // No exception thrown — test passes if we reach here
    }
}
