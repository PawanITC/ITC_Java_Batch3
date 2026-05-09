package com.itc.funkart.aggregator.repository;

import com.itc.funkart.aggregator.entity.RatingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingSummaryRepository extends JpaRepository<RatingSummary, Long> {
}
