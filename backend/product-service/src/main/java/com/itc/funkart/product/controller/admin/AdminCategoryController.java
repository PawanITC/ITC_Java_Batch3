package com.itc.funkart.product.controller.admin;

import com.itc.funkart.common.dto.auth.response.category.CategoryResponse;
import com.itc.funkart.common.dto.response.ApiResponse;
import com.itc.funkart.product.dto.request.CategoryRequest;
import com.itc.funkart.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>AdminCategoryController</h2>
 * <p>
 * Administrative API for Category lifecycle management.
 * Access is restricted to ensure system-wide category integrity.
 * </p>
 */
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Tag(name = "Admin Categories", description = "Restricted Category Management")
public class AdminCategoryController {

    private final CategoryService categoryService;

    /**
     * Creates and persists a new product category.
     *
     * @param request The category details (name, description).
     * @return 201 Created with the persisted {@link CategoryResponse}.
     */
    @PostMapping(produces = "application/json")
    @Operation(summary = "Create a new category")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Category successfully persisted"));
    }

    /**
     * Deletes a category by its identifier.
     *
     * @param id Target category ID.
     * @return 200 OK with success confirmation.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Category successfully removed"));
    }
}