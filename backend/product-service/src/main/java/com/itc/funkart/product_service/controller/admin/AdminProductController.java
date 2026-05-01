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

import static org.springframework.http.ResponseEntity.*;

/**
 * Administrative endpoints for product catalog maintenance.
 * <p>
 * Provides full CRUD capabilities for the product inventory, restricted to administrators.
 * </p>
 */
@RestController
@RequestMapping(value = "/admin/products", produces = "application/json")
@RequiredArgsConstructor
@Tag(name = "Admin Products", description = "Administrative Product Management")
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        // We return the DTO, but the 'Message' is a constant to prevent Reflected XSS
        return status(HttpStatus.CREATED)
                .body(new ApiResponse<>(productService.createProduct(request), "Resource successfully created"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ok(new ApiResponse<>(productService.updateProduct(id, request), "Resource successfully updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ok(new ApiResponse<>(null, "Resource successfully deleted"));
    }
}