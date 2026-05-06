package com.itc.funkart.product.controller;

import com.itc.funkart.common.dto.auth.response.category.CategoryResponse;
import com.itc.funkart.common.dto.response.ApiResponse;
import com.itc.funkart.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h2>CategoryController</h2>
 * <p>
 * Public API endpoints for browsing product categories.
 * This controller provides read-only access to the category structure
 * used for catalog navigation.
 * </p>
 */
@RestController
@RequestMapping(value = "/categories", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Public Category Browsing API")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Fetches all available product categories.
     *
     * @return {@link ResponseEntity} containing an {@link ApiResponse} with a list of {@link CategoryResponse}.
     */
    @GetMapping
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories(), "Categories retrieved successfully"));
    }

    /**
     * Fetches details for a single category by its unique identifier.
     *
     * @param id The database primary key of the category.
     * @return {@link ResponseEntity} containing an {@link ApiResponse} with the specific {@link CategoryResponse}.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryById(id), "Category details retrieved"));
    }
}