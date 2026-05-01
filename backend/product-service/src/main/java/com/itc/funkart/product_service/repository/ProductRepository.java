package com.itc.funkart.product_service.repository;

import com.itc.funkart.product_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link Product} entity management.
 * <p>
 * This repository utilizes JPQL FETCH joins to mitigate the N+1 select problem
 * and optimize JVM heap usage during DTO mapping.
 * </p>
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Finds a product by its slug, including all associated images.
     *
     * @param slug The unique SEO-friendly identifier.
     * @return An Optional containing the Product with hydrated images.
     */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.slug = :slug")
    Optional<Product> findBySlug(@Param("slug") String slug);

    /**
     * Retrieves all products ordered by creation date, pre-fetching images.
     * <p>
     * Use this method for public catalog browsing to ensure a single database
     * round-trip. Note: Large datasets should be used with Pagination (Pageable).
     * </p>
     *
     * @return List of products with images initialized.
     */
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.images " +
            "ORDER BY p.createdAt DESC")
    List<Product> findAllWithImages();
}