package com.itc.funkart.product.repository;

import com.itc.funkart.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing Product Categories.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Used by the DataSeeder to avoid duplicate category names on restart.
     */
    Optional<Category> findByName(String name);
}
