package com.itc.funkart.product_service.service;

import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.dto.response.CategoryResponse;
import org.apache.kafka.common.errors.ResourceNotFoundException;

import java.util.List;

/**
 * Service interface for managing product categories.
 * Operations are restricted based on the authenticated user's role
 * (handled via Security interceptors).
 */
public interface CategoryService {

    /**
     * Creates a new category.
     * @param request The category details.
     * @return The created category response.
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Retrieves all categories in a read-only optimized format.
     */
    List<CategoryResponse> getAllCategories();

    /**
     * Finds a specific category.
     * @throws ResourceNotFoundException if ID does not exist.
     */
    CategoryResponse getCategoryById(Long id);

    /**
     * Removes a category.
     * Implementations should handle referential integrity checks.
     */
    void deleteCategory(Long id);
}