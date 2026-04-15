package com.itc.catalogueservice.controller;

import com.itc.catalogueservice.dto.ProductDTO;
import com.itc.catalogueservice.response.ApiResponse;
import com.itc.catalogueservice.service.CatalogueService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/catalogue")
@Validated
public class CatalogueController {

    private final CatalogueService catalogueService;

    public CatalogueController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @GetMapping("/products")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ProductDTO>>>> getProducts(

            @RequestParam(required = false) String q,

            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be at least 1") Integer page,

            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1") @Max(value = 100, message = "Size must not exceed 100") Integer size,

            @RequestParam(required = false) String category,

            @RequestParam(required = false) @DecimalMin(value = "0.0", message = "Price cannot be negative") BigDecimal minPrice,

            @RequestParam(required = false) @DecimalMin(value = "0.0", message = "Price cannot be negative") BigDecimal maxPrice,

            @RequestParam(required = false) @Min(value = 0, message = "Rating must be at least 0") @Max(value = 5, message = "Rating must not exceed 5") Double rating) {

        return catalogueService.getProducts(q, page, size, category, minPrice, maxPrice, rating)
                .thenApply(products -> {

                    ApiResponse<List<ProductDTO>> response = new ApiResponse<>(HttpStatus.OK.value(),
                            "Products retrieved", products);

                    return ResponseEntity.ok(response);
                });
    }

    // Retrieves the top-selling products
    @GetMapping("/top-selling")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ProductDTO>>>> getTopSellingProducts(

            @RequestParam(defaultValue = "5") @Min(value = 1, message = "Limit must be at least 1") @Max(value = 100, message = "Limit must not exceed 100") Integer limit,

            @RequestParam(required = false) String category

    ) {
        return catalogueService.getTopSellingProducts(limit, category)
                .thenApply(products -> {

                    ApiResponse<List<ProductDTO>> response = new ApiResponse<>(HttpStatus.OK.value(),
                            "Top selling products retrieved", products);

                    return ResponseEntity.ok(response);
                });
    }


    @PostMapping("/test/order")
    public ResponseEntity<String> testOrder(
            @RequestParam String productId,
            @RequestParam int quantitySold,
            @RequestParam(required = false) String category) {

        catalogueService.handleOrderEvent(productId, quantitySold, category);
        return ResponseEntity.ok("Redis updated");
    }

}
