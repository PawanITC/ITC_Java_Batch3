package com.itc.funkart.serviceimpl;



import com.itc.funkart.entity.ProductRatingSummary;
import com.itc.funkart.projection.RatingStats;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import com.itc.funkart.repository.ReviewRepository;
import com.itc.funkart.service.RatingSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RatingSummaryServiceImpl implements RatingSummaryService {

    private final ReviewRepository reviewRepository;
    private final ProductRatingSummaryRepository summaryRepository;

    @Override
    public void recalculateSummary(Long productId) {

        RatingStats stats = reviewRepository.getRatingStatsByProductId(productId);

        if (stats == null || stats.getAvg() == null) {
            summaryRepository.deleteById(productId);
            return;
        }

        ProductRatingSummary summary = summaryRepository.findById(productId)
                .orElseGet(() -> new ProductRatingSummary(productId));

        summary.setAverageRating(stats.getAvg());
        summary.setRatingCount(stats.getCount());

        summaryRepository.save(summary);
    }
}

