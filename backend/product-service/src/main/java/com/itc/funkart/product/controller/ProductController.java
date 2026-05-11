package com.itc.funkart.product.controller;

import com.itc.funkart.common.dto.auth.response.product.ProductResponse;
import com.itc.funkart.common.dto.auth.response.product.ProductsResponse;
import com.itc.funkart.common.dto.response.ApiResponse;
import com.itc.funkart.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public API endpoints for browsing the product catalog.
 * Provides read-only access to product information for consumers.
 */
@RestController
@RequestMapping(value = "/products", produces = "application/json")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Public Product Browsing API")
public class ProductController {

    private final ProductService productService;

    /**
     * Retrieves the product catalog.
     * Supports optional filtering via query parameters:
     * ?search=keyword  — case-insensitive name substring match
     * ?categoryId=1    — filter by category ID
     * When neither param is present the cached full-catalog path is used.
     */
    @GetMapping
    @Operation(summary = "Get all products (with optional search/category filter)")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId) {

        boolean hasFilter = (search != null && !search.isBlank()) || categoryId != null;
        List<ProductResponse> products = hasFilter
                ? productService.getFilteredProducts(search, categoryId)
                : productService.getAllProducts();

        return ResponseEntity.ok(ApiResponse.success(products, "Current products list"));
    }

    /**
     * Retrieves a single product by its numeric ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        ProductResponse product = productService.getProduct(id);
        return ResponseEntity.ok(ApiResponse.success(product, "Product details retrieved"));
    }


    /**
     * Resolves a collection of IDs into Product details.
     * Renamed to /batch for architectural clarity.
     */
    @PostMapping("/batch")
    @Operation(summary = "Batch retrieve products by IDs")
    public ResponseEntity<ApiResponse<ProductsResponse>> getProductsByIds(@RequestBody List<Long> ids) {
        ProductsResponse productsResponse = productService.getProductsByIds(ids);
        return ResponseEntity.ok(ApiResponse.success(productsResponse, "Products by Id: " + ids + " retrieved"));
    }
}