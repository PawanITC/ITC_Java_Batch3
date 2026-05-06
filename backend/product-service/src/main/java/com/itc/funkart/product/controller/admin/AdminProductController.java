package com.itc.funkart.product.controller.admin;

import com.itc.funkart.common.dto.auth.response.product.ProductResponse;
import com.itc.funkart.common.dto.response.ApiResponse;
import com.itc.funkart.product.dto.request.ProductCreateRequest;
import com.itc.funkart.product.dto.request.ProductUpdateRequest;
import com.itc.funkart.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>AdminProductController</h2>
 * <p>
 * Administrative endpoints for the lifecycle management of products within the Funkart ecosystem.
 * Access to these endpoints is strictly restricted to users with administrative privileges.
 * </p>
 */
@RestController
@RequestMapping(value = "/admin/products", produces = "application/json")
@RequiredArgsConstructor
@Tag(name = "Admin Products", description = "Restricted Administrative Product Management")
public class AdminProductController {

    private final ProductService productService;

    /**
     * Creates and persists a new product in the catalog.
     *
     * @param request Validated product creation details.
     * @return {@link ResponseEntity} containing an {@link ApiResponse} with the created {@link ProductResponse}.
     */
    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Product successfully cataloged"));
    }

    /**
     * Updates an existing product's details by its unique identifier.
     *
     * @param id      The primary key of the product to modify.
     * @param request Validated update details (supports partial updates).
     * @return {@link ResponseEntity} containing an {@link ApiResponse} with the updated {@link ProductResponse}.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        ProductResponse updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Resource successfully updated"));
    }

    /**
     * Removes a product from the database and performs necessary cleanup.
     *
     * @param id The primary key of the product to delete.
     * @return {@link ResponseEntity} containing an empty {@link ApiResponse} indicating success.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Resource successfully deleted"));
    }
}