package com.itc.funkart.product_service.controller;

import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.dto.response.ProductsResponse;
import com.itc.funkart.product_service.response.ApiResponse;
import com.itc.funkart.product_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public API endpoints for browsing the product catalog.
 * <p>
 * Provides read-only access to product information for consumers.
 * </p>
 */
@RestController
@RequestMapping(value = "/products", produces = "application/json")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Public Product Browsing API")
public class ProductController {

    private final ProductService productService;

    /**
     * Retrieves the full catalog.
     * <p>Note: This response is cached at the service layer.</p>
     */
    @GetMapping
    @Operation(summary = "Get all products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        return ResponseEntity.ok(new ApiResponse<>(productService.getAllProducts()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(productService.getProduct(id)));
    }

    /**
     * Resolves a collection of IDs into Product details.
     * Often called by the Cart-Service or Order-Service.
     */
    @PostMapping("/by-ids")
    @Operation(summary = "Batch retrieve products by IDs")
    public ResponseEntity<ApiResponse<ProductsResponse>> getProductsByIds(@RequestBody List<Long> ids) {
        return ResponseEntity.ok(new ApiResponse<>(productService.getProductsByIds(ids)));
    }
}