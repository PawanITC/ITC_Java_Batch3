package com.itc.funkart.aggregator.service;

import com.itc.funkart.aggregator.dto.RatingSummaryResponse;
import com.itc.funkart.aggregator.entity.RatingSummary;
import com.itc.funkart.aggregator.repository.RatingSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingSummaryServiceTest {

    @Mock RatingSummaryRepository summaryRepository;
    @InjectMocks RatingSummaryService summaryService;

    @Test
    void getSummary_existingProduct_returnsSummary() {
        RatingSummary summary = RatingSummary.builder()
                .productId(5L).totalReviews(10).sumRatings(42)
                .oneStar(0).twoStar(1).threeStar(2).fourStar(4).fiveStar(3)
                .averageRating(4.2).build();

        when(summaryRepository.findById(5L)).thenReturn(Optional.of(summary));

        RatingSummaryResponse result = summaryService.getSummary(5L);

        assertThat(result.getProductId()).isEqualTo(5L);
        assertThat(result.getTotalReviews()).isEqualTo(10);
        assertThat(result.getAverageRating()).isEqualTo(4.2);
        assertThat(result.getFiveStar()).isEqualTo(3);
        assertThat(result.getOneStar()).isEqualTo(0);
    }

    @Test
    void getSummary_productWithNoReviews_returnsZeroedSummary() {
        when(summaryRepository.findById(99L)).thenReturn(Optional.empty());

        RatingSummaryResponse result = summaryService.getSummary(99L);

        assertThat(result.getProductId()).isEqualTo(99L);
        assertThat(result.getTotalReviews()).isEqualTo(0);
        assertThat(result.getAverageRating()).isEqualTo(0.0);
    }
}
