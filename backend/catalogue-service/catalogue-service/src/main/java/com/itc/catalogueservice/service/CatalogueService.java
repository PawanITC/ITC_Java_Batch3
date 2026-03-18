package com.itc.catalogueservice.service;

import com.itc.catalogueservice.dto.ProductDTO;
import com.itc.catalogueservice.client.ProductApiClient;
import com.itc.catalogueservice.exception.catalogue.NoProductsException;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class CatalogueService {
    private final ProductApiClient productApiClient;

    public CatalogueService(ProductApiClient productApiClient){
        this.productApiClient = productApiClient;
    }

    public CompletableFuture<List<ProductDTO>> getProducts(
            Integer page, Integer size, String category,
            BigDecimal minPrice, BigDecimal maxPrice, Double rating) {

        return productApiClient.getProducts()
                .thenApply(products -> {

                    if (products.isEmpty()) {
                        throw new NoProductsException();
                    }

                    return products;
                });
    }
}