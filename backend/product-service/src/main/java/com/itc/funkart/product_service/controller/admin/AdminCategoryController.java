package com.itc.funkart.product_service.controller.admin;

import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.dto.response.CategoryResponse;
import com.itc.funkart.product_service.response.ApiResponse;
import com.itc.funkart.product_service.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Administrative endpoints for managing product categories.
 * <p>
 * Access is restricted to users with {@code ROLE_ADMIN} authority.
 * </p>
 */
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Tag(name = "Admin Categories", description = "Administrative Category Management")
public class AdminCategoryController {

    private final CategoryService categoryService;

    /**
     * Creates a new product category.
     *
     * @param request The category details.
     * @return Standardized response containing the created category.
     */
    @PostMapping
    @Operation(summary = "Create a new category")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(categoryService.createCategory(request), "Category created successfully"));
    }

    /**
     * Deletes an existing category.
     *
     * @param id The unique identifier of the category.
     * @return Empty success response.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(new ApiResponse<>(null, "Category deleted successfully"));
    }
}