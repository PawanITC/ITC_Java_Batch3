package com.itc.funkart.product.repository;

import com.itc.funkart.product.entity.Product;
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
     * Cache miss triggers a JOIN FETCH to hydrate images in a single round-trip.
     *
     * @return List of products with images initialized.
     */
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.images " +
            "ORDER BY p.createdAt DESC")
    List<Product> findAllWithImages();

    /**
     * Filtered catalog query — supports optional name search and category filter.
     * <p>
     * IMPORTANT: pass empty string ("") when no search term is provided — NOT null.
     * PostgreSQL JDBC infers a null String parameter as bytea, causing
     * "function lower(bytea) does not exist". The empty-string sentinel avoids this.
     * </p>
     *
     * @param search     Empty string to skip filter, otherwise case-insensitive substring match.
     * @param categoryId Exact category match (nullable — null skips filter).
     * @return Filtered list of products with images pre-fetched.
     */
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.images " +
            "WHERE (:search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "ORDER BY p.createdAt DESC")
    List<Product> findFilteredWithImages(@Param("search") String search,
                                         @Param("categoryId") Long categoryId);
}
