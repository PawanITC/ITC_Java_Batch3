package com.itc.funkart.service;



import com.itc.funkart.dto.ProductRatingSummaryResponse;

public interface RatingSummaryService {

    ProductRatingSummaryResponse getRatingSummary(Long productId);
}
