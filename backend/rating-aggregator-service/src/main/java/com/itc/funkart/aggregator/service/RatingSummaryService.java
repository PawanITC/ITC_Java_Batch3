package com.itc.funkart.aggregator.service;

import com.itc.funkart.aggregator.dto.RatingSummaryResponse;
import com.itc.funkart.aggregator.repository.RatingSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingSummaryService {

    private final RatingSummaryRepository summaryRepository;

    @Transactional(readOnly = true)
    public RatingSummaryResponse getSummary(Long productId) {
        return summaryRepository.findById(productId)
                .map(RatingSummaryResponse::from)
                .orElseGet(() -> RatingSummaryResponse.empty(productId));
    }
}
