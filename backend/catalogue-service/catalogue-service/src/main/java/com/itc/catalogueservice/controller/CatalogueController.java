package com.itc.catalogueservice.controller;

import com.itc.catalogueservice.dto.ProductDTO;
import com.itc.catalogueservice.response.ApiResponse;
import com.itc.catalogueservice.service.CatalogueService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/catalogue")
public class CatalogueController {

    private final CatalogueService catalogueService;

            public CatalogueController(CatalogueService catalogueService1){
                this.catalogueService = catalogueService1;
            };

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProducts() {

        List<ProductDTO> products = catalogueService.getProducts();

        ApiResponse<List<ProductDTO>> response =
                new ApiResponse<>(200, "Products fetched successfully", products);

        return ResponseEntity.ok(response);
    }





}
