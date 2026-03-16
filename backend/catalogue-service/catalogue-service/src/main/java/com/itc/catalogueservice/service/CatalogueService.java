package com.itc.catalogueservice.service;

import com.itc.catalogueservice.dto.ProductDTO;
import com.itc.catalogueservice.client.ProductApiClient;
import com.itc.catalogueservice.exception.catalogue.NoProductsException;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class CatalogueService {
    private final ProductApiClient productApiClient;

    public CatalogueService(ProductApiClient productApiClient){
        this.productApiClient = productApiClient;
    }

    public List<ProductDTO> getProducts() {

        List<ProductDTO> products = productApiClient.getProducts();

        if (products.isEmpty()) {
            throw new NoProductsException();
        }

        return products;
    }
}