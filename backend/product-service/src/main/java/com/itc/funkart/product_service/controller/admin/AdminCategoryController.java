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
 * Administrative API for Category lifecycle management.
 * <p>
 * This controller is isolated from the public browsing API to ensure
 * strict 'ROLE_ADMIN' authorization via the security filter chain.
 * </p>
 */
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Tag(name = "Admin Categories", description = "Restricted Category Management")
public class AdminCategoryController {

    private final CategoryService categoryService;

    /**
     * Creates a new product category.
     * <p>
     * Note: Input is validated via @Valid to prevent Malformed Data Injection.
     * To address XSS concerns, the service layer ensures name sanitization.
     * </p>
     *
     * @param request The category details.
     * @return 201 Created and the new Category resource.
     */
    @PostMapping(produces = "application/json")
    @Operation(summary = "Create a new category")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(created, "Resource successfully persisted"));
    }

    /**
     * Deletes a category by ID.
     * <p>
     * Referential integrity is checked at the Service/DB level.
     * </p>
     *
     * @param id Target category ID.
     * @return 200 OK.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(new ApiResponse<>(null, "Resource successfully removed"));
    }
}