package com.itc.funkart.product_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.dto.response.ProductsResponse;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.exceptions.BadRequestException;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductResponse createProductResponse(Long id, String name) {
        return ProductResponse.builder()
                .id(id)
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .description("Product description")
                .price(BigDecimal.valueOf(99.99))
                .categoryName("Electronics")
                .active(true)
                .brand("TestBrand")
                .build();
    }

    private Product createProduct(Long id, String name) {
        return Product.builder()
                .id(id)
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .description("Product description")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .active(true)
                .brand("TestBrand")
                .build();
    }

    @Test
    @DisplayName("Should get all products successfully")
    void shouldGetAllProductsSuccessfully() throws Exception {
        // Arrange
        ProductResponse prod1 = createProductResponse(1L, "Laptop");
        ProductResponse prod2 = createProductResponse(2L, "Phone");
        List<ProductResponse> products = List.of(prod1, prod2);

        when(productService.getAllProducts()).thenReturn(products);

        // Act & Assert
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("Phone"));

        verify(productService, times(1)).getAllProducts();
    }

    @Test
    @DisplayName("Should return empty list when no products exist")
    void shouldReturnEmptyListWhenNoProductsExist() throws Exception {
        // Arrange
        when(productService.getAllProducts()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(productService, times(1)).getAllProducts();
    }

    @Test
    @DisplayName("Should get product by id successfully")
    void shouldGetProductByIdSuccessfully() throws Exception {
        // Arrange
        ProductResponse product = createProductResponse(1L, "Laptop");
        when(productService.getProduct(1L)).thenReturn(product);

        // Act & Assert
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(99.99));

        verify(productService, times(1)).getProduct(1L);
    }

    @Test
    @DisplayName("Should return 404 when product not found")
    void shouldReturn404WhenProductNotFound() throws Exception {
        // Arrange
        when(productService.getProduct(anyLong()))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());

        verify(productService, times(1)).getProduct(999L);
    }

    @Test
    @DisplayName("Should get products by ids successfully")
    void shouldGetProductsByIdsSuccessfully() throws Exception {
        // Arrange
        Product prod1 = createProduct(1L, "Laptop");
        Product prod2 = createProduct(2L, "Phone");

        ProductsResponse response = new ProductsResponse();
        response.setFound(List.of(prod1, prod2));
        response.setMissing(List.of());

        when(productService.getProductsByIds(anyList())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/products/by-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found[0].id").value(1L))
                .andExpect(jsonPath("$.found[1].id").value(2L))
                .andExpect(jsonPath("$.missing.length()").value(0));

        verify(productService, times(1)).getProductsByIds(anyList());
    }

    @Test
    @DisplayName("Should return missing ids in products response")
    void shouldReturnMissingIdsInProductsResponse() throws Exception {
        // Arrange
        Product prod1 = createProduct(1L, "Laptop");

        ProductsResponse response = new ProductsResponse();
        response.setFound(List.of(prod1));
        response.setMissing(List.of(2L, 3L));

        when(productService.getProductsByIds(anyList())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/products/by-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(1L, 2L, 3L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found[0].id").value(1L))
                .andExpect(jsonPath("$.missing[0]").value(2L))
                .andExpect(jsonPath("$.missing[1]").value(3L));

        verify(productService, times(1)).getProductsByIds(anyList());
    }

    @Test
    @DisplayName("Should handle bad request for empty ids list")
    void shouldHandleBadRequestForEmptyIdsList() throws Exception {
        // Arrange
        when(productService.getProductsByIds(anyList()))
                .thenThrow(new BadRequestException("ID list cannot be empty"));

        // Act & Assert
        mockMvc.perform(post("/api/products/by-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of())))
                .andExpect(status().isBadRequest());

        verify(productService, times(1)).getProductsByIds(anyList());
    }

    @Test
    @DisplayName("Should return correct content type for products")
    void shouldReturnCorrectContentTypeForProducts() throws Exception {
        // Arrange
        ProductResponse product = createProductResponse(1L, "Laptop");
        when(productService.getProduct(1L)).thenReturn(product);

        // Act & Assert
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));

        verify(productService, times(1)).getProduct(1L);
    }

    @Test
    @DisplayName("Should handle multiple product requests")
    void shouldHandleMultipleProductRequests() throws Exception {
        // Arrange
        ProductResponse prod1 = createProductResponse(1L, "Laptop");
        ProductResponse prod2 = createProductResponse(2L, "Phone");

        when(productService.getProduct(1L)).thenReturn(prod1);
        when(productService.getProduct(2L)).thenReturn(prod2);

        // Act & Assert
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));

        mockMvc.perform(get("/api/products/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Phone"));

        verify(productService, times(1)).getProduct(1L);
        verify(productService, times(1)).getProduct(2L);
    }

    @Test
    @DisplayName("Should return valid product fields in response")
    void shouldReturnValidProductFieldsInResponse() throws Exception {
        // Arrange
        ProductResponse product = createProductResponse(1L, "Laptop");
        when(productService.getProduct(1L)).thenReturn(product);

        // Act & Assert
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.slug").value("laptop"))
                .andExpect(jsonPath("$.price").value(99.99))
                .andExpect(jsonPath("$.active").value(true));

        verify(productService, times(1)).getProduct(1L);
    }

    @Test
    @DisplayName("Should handle post request with valid json")
    void shouldHandlePostRequestWithValidJson() throws Exception {
        // Arrange
        Product prod1 = createProduct(1L, "Laptop");
        ProductsResponse response = new ProductsResponse();
        response.setFound(List.of(prod1));
        response.setMissing(List.of());

        when(productService.getProductsByIds(List.of(1L))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/products/by-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[1]"))
                .andExpect(status().isOk());

        verify(productService, times(1)).getProductsByIds(anyList());
    }
}

