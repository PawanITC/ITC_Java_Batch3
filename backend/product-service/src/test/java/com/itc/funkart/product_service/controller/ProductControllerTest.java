package com.itc.funkart.product_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.product_service.config.SecurityConfig;
import com.itc.funkart.product_service.controller.admin.AdminProductController;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.response.ProductsResponse;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.service.JwtService;
import com.itc.funkart.product_service.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>ProductControllerTest</h2>
 * Comprehensive unit tests for Product and Admin Product controllers.
 * Validates catalog browsing, bulk lookups, and role-based access control.
 */
@WebMvcTest(controllers = {ProductController.class, AdminProductController.class})
@ActiveProfiles("test")
@Import({SecurityConfig.class})
@DisplayName("Product Service Web Layer Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    /**
     * Satisfying Spring Security's dependency on a user source.
     * By providing a MockitoBean, we prevent Spring Boot from generating a
     * default "user" with a random password in the logs.
     */
    @MockitoBean
    private UserDetailsService userDetailsService;

    // --- PUBLIC CATALOG TESTS ---

    /**
     * Verifies that bulk product lookups handle scenarios where some IDs are not found.
     */
    @Test
    @DisplayName("POST /products/by-ids - Branch: Mixed Results")
    void shouldHandleMixedBulkResults() throws Exception {
        ProductsResponse response = ProductsResponse.builder()
                .found(List.of())
                .missing(List.of(1L, 2L))
                .build();

        when(productService.getProductsByIds(anyList())).thenReturn(response);

        mockMvc.perform(post("/products/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.missing.length()").value(2))
                .andExpect(jsonPath("$.message").value("Operation successful"));
    }

    /**
     * Validates that the GlobalExceptionHandler correctly formats 404 responses
     * into the standardized ApiResponse error structure.
     */
    @Test
    @DisplayName("GET /products/{id} - Branch: Not Found")
    void shouldReturn404ForInvalidId() throws Exception {
        when(productService.getProduct(99L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.message").value("Product not found with id: 99"))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // --- ADMIN ACCESS CONTROL TESTS ---

    /**
     * Verifies that users with ROLE_ADMIN can successfully access management endpoints.
     */
    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("POST /admin/products - Authorized Access")
    void shouldAllowAdminToCreateProduct() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Gaming Laptop",
                "High end",
                new BigDecimal("1500.00"),
                10,
                1L,
                List.of(),   // Image URLs (List<String>)
                "Alienware"  // Brand (String)
        );

        mockMvc.perform(post("/admin/products")
                        .with(csrf()) // Required when SecurityConfig is active
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    /**
     * Ensures that users without sufficient privileges (ROLE_USER) receive a 403 Forbidden.
     */
    @Test
    @WithMockUser(authorities = "ROLE_USER")
    @DisplayName("POST /admin/products - Forbidden for ROLE_USER")
    void shouldDenyUserFromCreatingProduct() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Gaming Laptop",
                "High end",
                new BigDecimal("1500.00"),
                10,
                1L,
                List.of(),   // Image URLs (List<String>)
                "Alienware"  // Brand (String)
        );

        mockMvc.perform(post("/admin/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /**
     * Verifies that unauthenticated requests to protected endpoints are rejected.
     */
    @Test
    @DisplayName("POST /admin/products - Unauthorized (No User)")
    void shouldDenyAnonymousFromCreatingProduct() throws Exception {
        mockMvc.perform(post("/admin/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}