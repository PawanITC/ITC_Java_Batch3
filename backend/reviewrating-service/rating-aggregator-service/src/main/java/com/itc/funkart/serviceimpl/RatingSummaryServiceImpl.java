package com.itc.funkart.serviceimpl;

import com.itc.funkart.dto.RatingStatsDto;
import com.itc.funkart.model.ProductRatingSummary;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import com.itc.funkart.repository.ReviewRepository;
import com.itc.funkart.service.RatingSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingSummaryServiceImpl implements RatingSummaryService {

    private final ReviewRepository reviewRepository;
    private final ProductRatingSummaryRepository summaryRepository;

    @Override
    @Transactional
    public void recalculateSummary(Long productId) {
        RatingStatsDto stats = reviewRepository.getRatingStatsByProductId(productId);

        if (stats == null || stats.getAvg() == null) {
            log.info("No reviews found for product {} — removing summary.", productId);
            summaryRepository.deleteById(productId);
            return;
        }

        ProductRatingSummary summary = summaryRepository.findById(productId)
                .orElseGet(() -> new ProductRatingSummary(productId));

        summary.setAverageRating(stats.getAvg());
        summary.setRatingCount(stats.getCount());
        summaryRepository.save(summary);

        log.info("Updated rating summary productId={} avg={} count={}",
                productId, stats.getAvg(), stats.getCount());
    }
}
