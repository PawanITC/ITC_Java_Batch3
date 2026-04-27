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
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Public Product Browsing API")
public class ProductController {

    private final ProductService productService;

    /**
     * Retrieves a complete list of active products in the catalog.
     *
     * @return List of {@link ProductResponse} wrapped in a standardized API response.
     */
    @GetMapping
    @Operation(summary = "Get all products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        return ResponseEntity.ok(new ApiResponse<>(productService.getAllProducts()));
    }

    /**
     * Retrieves detailed information for a specific product.
     *
     * @param id The unique product identifier.
     * @return Detailed {@link ProductResponse}.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(productService.getProduct(id)));
    }

    /**
     * Performs a batch lookup of products based on a list of IDs.
     * Useful for recovering cart state or populating order summaries.
     *
     * @param ids List of product IDs to retrieve.
     * @return {@link ProductsResponse} containing the matching product details.
     */
    @PostMapping("/by-ids")
    @Operation(summary = "Batch retrieve products by IDs")
    public ResponseEntity<ApiResponse<ProductsResponse>> getProductsByIds(@RequestBody List<Long> ids) {
        return ResponseEntity.ok(new ApiResponse<>(productService.getProductsByIds(ids)));
    }
}