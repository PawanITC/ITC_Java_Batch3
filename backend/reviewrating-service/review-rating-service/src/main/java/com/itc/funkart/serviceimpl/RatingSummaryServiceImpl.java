package com.itc.funkart.serviceimpl;

import com.itc.funkart.dto.ProductRatingSummaryResponse;
import com.itc.funkart.entity.ProductRatingSummary;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import com.itc.funkart.service.RatingSummaryService;
import org.springframework.stereotype.Service;

@Service
public class RatingSummaryServiceImpl implements com.itc.funkart.service.RatingSummaryService {

    private final com.itc.funkart.repository.ProductRatingSummaryRepository summaryRepository;

    public RatingSummaryServiceImpl(ProductRatingSummaryRepository summaryRepository) {
        this.summaryRepository = summaryRepository;
    }

    @Override
    public ProductRatingSummaryResponse getRatingSummary(Long productId) {
        ProductRatingSummary summary = summaryRepository.findById(productId)
                .orElseGet(() -> {
                    ProductRatingSummary s = new ProductRatingSummary();
                    s.setProductId(productId);
                    s.setAverageRating(0.0);
                    s.setRatingCount(0);
                    return s;
                });

        ProductRatingSummaryResponse resp = new ProductRatingSummaryResponse();
        resp.setProductId(summary.getProductId());
        resp.setAverageRating(summary.getAverageRating());
        resp.setRatingCount(summary.getRatingCount());
        return resp;
    }
}
