package com.itc.funkart.repository;

import com.itc.funkart.entity.ProductRatingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRatingSummaryRepository extends JpaRepository<ProductRatingSummary, Long> {
}
