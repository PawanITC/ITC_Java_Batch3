package com.itc.catalogueservice.client;

import com.itc.catalogueservice.dto.ProductDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class ProductApiClientTest {

    private final ProductApiClient client =
            new ProductApiClient(Executors.newSingleThreadExecutor());

    @Test
    void getProducts_shouldReturnProducts() {

        CompletableFuture<List<ProductDTO>> result = client.getProducts();

        List<ProductDTO> products = result.join();

        assertNotNull(products);
        assertFalse(products.isEmpty());
        assertEquals(20, products.size());
    }
}