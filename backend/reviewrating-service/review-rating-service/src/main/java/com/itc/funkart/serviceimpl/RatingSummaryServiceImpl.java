package com.itc.funkart.serviceimpl;

import com.itc.funkart.repository.ProductRatingSummaryRepository;
import com.itc.funkart.repository.ReviewRepository;
import com.itc.funkart.service.RatingSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingSummaryServiceImpl implements RatingSummaryService {

    private final ReviewRepository reviewRepository;
    private final ProductRatingSummaryRepository summaryRepository;

    @Override
    public void recalculateSummary(Long productId) {

        /*RatingStatsDto stats = reviewRepository.getRatingStatsByProductId(productId);

        if (stats == null || stats.avg() == null) {
            log.info("No reviews found for product {}. Deleting summary.", productId);
            summaryRepository.deleteById(productId);
            return;
        }

        ProductRatingSummary summary = summaryRepository.findById(productId)
                .orElseGet(() -> new ProductRatingSummary(productId));

        summary.setAverageRating(stats.avg());
        summary.setRatingCount(stats.count());

        summaryRepository.save(summary);

        log.info("Updated summary for product {} -> avg={}, count={}",
                productId, stats.avg(), stats.count());*/
    }
}
