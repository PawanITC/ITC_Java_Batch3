//Tests for the CatalogueService
package com.itc.catalogueservice.service;

import com.itc.catalogueservice.client.ProductApiClient;
import com.itc.catalogueservice.dto.ProductDTO;
import com.itc.catalogueservice.exception.catalogue.NoProductsException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogueServiceTest {

    private final ProductApiClient productApiClient = mock(ProductApiClient.class);
    private final CatalogueService catalogueService = new CatalogueService(productApiClient);

    //Should test for not null and not empty when products are returned
    @Test
    void getProducts_shouldReturnProducts() {

        when(productApiClient.getProducts()).thenReturn(List.of(new ProductDTO()));
        List<ProductDTO> products = catalogueService.getProducts();

        assertNotNull(products);
        assertFalse(products.isEmpty());
    }


    //Should test for when products are not returned and method throws NoProductsException
    @Test
    void getProducts_shouldThrowNoProductsException(){
        when(productApiClient.getProducts()).thenReturn(List.of());
        assertThrows(NoProductsException.class, catalogueService::getProducts);
    }
}