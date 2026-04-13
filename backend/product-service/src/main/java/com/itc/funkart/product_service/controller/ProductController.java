package com.itc.funkart.product_service.controller;

import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.request.ProductUpdateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.dto.response.ProductsResponse;
import com.itc.funkart.product_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * Product Controller
 * 
 * REST API endpoints for managing products in the e-commerce platform.
 * Provides operations for creating, retrieving, updating, and deleting products.
 * 
 * Base URL: /api/products
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(
    name = "Products",
    description = "Product Management API - Create, read, update, and delete products"
)

import java.util.List;
import org.springframework.web.bind.annotation.*;
//@RestController
//@RequestMapping("/api/products")
//@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:5173")
//public class ProductController {
//
//    private final ProductService productService;
//
//    @PostMapping
//    public ResponseEntity<ProductResponse> createProduct(
//            @Valid @RequestBody ProductCreateRequest request) {
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(productService.createProduct(request));
//    }
//
//    @GetMapping(value = "/{id}")
//    public ResponseEntity<ProductResponse> getProduct(
//            @PathVariable Long id) {
//
//        return ResponseEntity
//                .status(HttpStatus.OK)
//                .body(productService.getProduct(id));
//    }
//
//    @GetMapping
//    public ResponseEntity<List<ProductResponse>> getAllProducts() {
//
//        return ResponseEntity
//                .status(HttpStatus.OK)
//                .body(productService.getAllProducts());
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<ProductResponse> updateProduct(
//            @PathVariable Long id,
//            @Valid @RequestBody ProductUpdateRequest request) {
//
//        return ResponseEntity
//                .status(HttpStatus.OK)
//                .body(productService.updateProduct(id, request));
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteProduct(
//            @PathVariable Long id) {
//
//        productService.deleteProduct(id);
//
//        return ResponseEntity
//                .status(HttpStatus.NO_CONTENT)
//                .build();
//    }
//}

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    private final ProductService productService;

    /**
     * Retrieve all products
     * 
     * Fetches a list of all available products in the system.
     * Results are cached for improved performance.
     * 
     * @return List of all products with their details
     */
    @GetMapping
    @Operation(
        summary = "Get all products",
        description = "Retrieve a list of all available products in the e-commerce platform. Results are cached.",
        tags = {"Products", "Retrieve"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all products",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * Retrieve a specific product by ID
     * 
     * Fetches detailed information about a single product including:
     * - Basic product info (name, description, price)
     * - Category information
     * - Product images
     * - Stock status
     * 
     * @param id The unique product identifier (Long)
     * @return Product details if found
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get product by ID",
        description = "Retrieve detailed information about a specific product by its ID. " +
                      "Includes product images, category, and all metadata.",
        tags = {"Products", "Retrieve"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product found and returned successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found with the given ID"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<ProductResponse> getProduct(
        @Parameter(
            name = "id",
            description = "The unique identifier of the product to retrieve",
            required = true,
            example = "1"
        )
        @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    /**
     * Retrieve multiple products by IDs
     * 
     * Batch retrieve products by providing a list of product IDs.
     * This is useful for cart operations or displaying multiple products.
     * 
     * @param ids List of product IDs to retrieve
     * @return Products found and their details
     */
    @PostMapping("/by-ids")
    @Operation(
        summary = "Get products by multiple IDs",
        description = "Retrieve multiple products in a single request by providing a list of product IDs. " +
                      "Useful for cart operations and batch retrieval.",
        tags = {"Products", "Retrieve", "Batch"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Products retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductsResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request body or IDs"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<ProductsResponse> getProductsByIds(
        @Parameter(
            name = "ids",
            description = "List of product IDs to retrieve",
            required = true,
            example = "[1, 2, 3]"
        )
        @RequestBody List<Long> ids) {
        return ResponseEntity.ok(productService.getProductsByIds(ids));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }
}
