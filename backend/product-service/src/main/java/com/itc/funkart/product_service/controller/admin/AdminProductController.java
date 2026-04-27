package com.itc.funkart.product_service.controller.admin;

import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.request.ProductUpdateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.response.ApiResponse;
import com.itc.funkart.product_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Administrative endpoints for product catalog maintenance.
 * <p>
 * Provides full CRUD capabilities for the product inventory, restricted to administrators.
 * </p>
 */
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Tag(name = "Admin Products", description = "Administrative Product Management")
public class AdminProductController {

    private final ProductService productService;

    /**
     * Persists a new product into the catalog.
     *
     * @param request The product details.
     * @return Standardized response with the persisted product.
     */
    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(productService.createProduct(request), "Product created successfully"));
    }

    /**
     * Updates an existing product's attributes.
     *
     * @param id      The product ID.
     * @param request The updated data.
     * @return Standardized response with the updated product.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(productService.updateProduct(id, request), "Product updated successfully"));
    }

    /**
     * Removes a product from the catalog.
     *
     * @param id The product ID.
     * @return Empty success response.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product from the catalog")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(new ApiResponse<>(null, "Product deleted successfully"));
    }
}