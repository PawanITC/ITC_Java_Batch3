package com.itc.funkart.product_service.service;

import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.dto.response.CategoryResponse;

import java.util.List;

/**
 * Service interface for managing product categories.
 */
public interface CategoryService {

    /**
     * Creates a new category for product organization.
     * * @param request The category name and description.
     *
     * @return The created category details.
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Fetches all available categories.
     * * @return List of category responses.
     */
    List<CategoryResponse> getAllCategories();

    /**
     * Retrieves a specific category by its ID.
     * * @param id The unique category identifier.
     *
     * @return The category details.
     */
    CategoryResponse getCategoryById(Long id);

    /**
     * Permanently removes a category.
     * * @param id The ID of the category to delete.
     */
    void deleteCategory(Long id);
}