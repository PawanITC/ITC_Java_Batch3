//Tests for the CatalogueService
package com.itc.catalogueservice.service;

import com.itc.catalogueservice.client.ProductApiClient;
import com.itc.catalogueservice.dto.ProductDTO;
import com.itc.catalogueservice.exception.catalogue.NoProductsException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogueServiceTest {

    private final ProductApiClient productApiClient;
    private final StringRedisTemplate redisTemplate;
    private final CatalogueService catalogueService;

    public CatalogueServiceTest() {
        this.productApiClient = mock(ProductApiClient.class);
        this.redisTemplate = mock(StringRedisTemplate.class);
        this.catalogueService = new CatalogueService(productApiClient, redisTemplate);
    }

    //Should test for not null and not empty when products are returned
    @Test
    void getProducts_shouldReturnProducts() {

        when(productApiClient.getProducts())
                .thenReturn(CompletableFuture.completedFuture(List.of(new ProductDTO())));

        List<ProductDTO> products =
                catalogueService.getProducts(1,10,null,null,null,null).join();

        assertNotNull(products);
        assertFalse(products.isEmpty());
    }

    //Should test for when products are not returned and method throws NoProductsException
    @Test
    void getProducts_shouldThrowNoProductsException(){

        when(productApiClient.getProducts())
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> catalogueService.getProducts(1,10,null,null,null,null).join()
        );

        assertTrue(ex.getCause() instanceof NoProductsException);
    }
}