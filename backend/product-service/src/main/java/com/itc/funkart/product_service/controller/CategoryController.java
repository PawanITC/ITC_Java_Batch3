package com.itc.funkart.product_service.controller;
import com.itc.funkart.product_service.dto.request.CategoryResponse;
import com.itc.funkart.product_service.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * Category Controller
 * 
 * REST API endpoints for managing product categories.
 * Provides operations for retrieving and organizing product categories.
 * 
 * Base URL: /api/categories
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(
    name = "Categories",
    description = "Category Management API - Retrieve and manage product categories"
)
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Retrieve all product categories
     * 
     * Fetches a complete list of all product categories available in the system.
     * Categories are used to organize and filter products.
     * 
     * @return List of all available categories
     */
    @GetMapping
    @Operation(
        summary = "Get all categories",
        description = "Retrieve a complete list of all product categories. Used for product organization and filtering.",
        tags = {"Categories", "Retrieve"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all categories",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * Retrieve a specific category by ID
     * 
     * Fetches detailed information about a single category including:
     * - Category name and description
     * - Associated products count (if available)
     * 
     * @param id The unique category identifier (Long)
     * @return Category details if found
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get category by ID",
        description = "Retrieve detailed information about a specific category by its ID. " +
                      "Includes category metadata and associated information.",
        tags = {"Categories", "Retrieve"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Category found and returned successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Category not found with the given ID"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<CategoryResponse> getCategory(
        @Parameter(
            name = "id",
            description = "The unique identifier of the category to retrieve",
            required = true,
            example = "1"
        )
        @PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }
}
