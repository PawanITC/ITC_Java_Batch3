package com.itc.funkart.product_service.controller;

import com.itc.funkart.product_service.dto.response.CategoryResponse;
import com.itc.funkart.product_service.response.ApiResponse;
import com.itc.funkart.product_service.service.CategoryService;
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
 * Public API endpoints for browsing product categories.
 * Isolated from administrative write-operations for better security partitioning.
 */
@RestController
@RequestMapping(value = "/categories", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Public Category Browsing API")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Fetches all available product categories.
     * Use this for top-level navigation menus.
     *
     * @return Standardized API response containing a list of Category DTOs.
     */
    @GetMapping
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        // Service layer should handle the @Transactional(readOnly = true) optimization
        return ResponseEntity.ok(new ApiResponse<>(categoryService.getAllCategories()));
    }

    /**
     * Fetches details for a single category.
     *
     * @param id The database primary key of the category.
     * @return Standardized API response containing the specific Category DTO.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(categoryService.getCategoryById(id)));
    }
}