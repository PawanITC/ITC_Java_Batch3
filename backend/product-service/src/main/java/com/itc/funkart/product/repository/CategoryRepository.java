package com.itc.funkart.product.repository;

import com.itc.funkart.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing Product Categories.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
