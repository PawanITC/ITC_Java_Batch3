package com.itc.funkart.serviceimpl;



import com.itc.funkart.dto.RatingStatsDto;
import com.itc.funkart.model.ProductRatingSummary;
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

        // Fetch aggregated stats from review-rating-service DB
        RatingStatsDto stats = reviewRepository.getRatingStatsByProductId(productId);

        // If no reviews exist → delete summary
        if (stats == null || stats.getAvg() == null) {
            log.info("No reviews found for product {}. Removing summary.", productId);
            summaryRepository.deleteById(productId);
            return;
        }

        // Load existing summary or create new one
        ProductRatingSummary summary = summaryRepository.findById(productId)
                .orElseGet(() -> new ProductRatingSummary(productId));

        // Update summary fields
        summary.setAverageRating(stats.getAvg());
        summary.setRatingCount(stats.getCount());

        // Save updated summary
        summaryRepository.save(summary);

        log.info("Updated rating summary for product {} → avg={}, count={}",
                productId, stats.getAvg(), stats.getCount());
    }
}

