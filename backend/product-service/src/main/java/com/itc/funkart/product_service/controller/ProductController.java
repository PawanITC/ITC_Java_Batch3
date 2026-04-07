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

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }
    @PostMapping("/by-ids")
    public ResponseEntity<ProductsResponse> getProductsByIds(@RequestBody List<Long> ids) {
        return ResponseEntity.ok(productService.getProductsByIds(ids));
    }
}
