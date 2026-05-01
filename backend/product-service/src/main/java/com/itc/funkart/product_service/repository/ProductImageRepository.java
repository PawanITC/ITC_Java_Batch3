package com.itc.funkart.product_service.repository;

import com.itc.funkart.product_service.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link ProductImage} persistence.
 * Typically used for direct image management or cleanup tasks.
 */
@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
}