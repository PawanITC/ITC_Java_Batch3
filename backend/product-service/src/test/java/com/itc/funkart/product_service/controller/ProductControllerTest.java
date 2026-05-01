package com.itc.funkart.product_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.product_service.config.SecurityConfig;
import com.itc.funkart.product_service.controller.admin.AdminProductController;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.request.ProductUpdateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.service.JwtService;
import com.itc.funkart.product_service.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>ProductControllerTest</h2>
 * Validates Catalog browsing, Administrative actions, and Security constraints.
 */
@WebMvcTest(controllers = {ProductController.class, AdminProductController.class})
@ActiveProfiles("test")
@Import({SecurityConfig.class})
@DisplayName("Product Web Layer - Coverage & Security")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("Public Browsing Endpoints")
    class PublicCatalog {

        @Test
        @DisplayName("GET /products/{id}: Success path")
        void getProductSuccess() throws Exception {
            ProductResponse resp = ProductResponse.builder().id(1L).name("Test").build();
            when(productService.getProduct(1L)).thenReturn(resp);

            mockMvc.perform(get("/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Test"));
        }

        @Test
        @DisplayName("POST /products/by-ids: Handle empty ID list")
        void handleEmptyBatchRequest() throws Exception {
            // Testing how the API responds when no IDs are sent
            mockMvc.perform(post("/products/by-ids")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Collections.emptyList())))
                    .andExpect(status().isOk());
            // Note: In Lesson 4, we might decide this should be a 400 Bad Request
        }
    }

    @Nested
    @DisplayName("Admin Management Endpoints")
    class AdminOperations {

        private ProductCreateRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new ProductCreateRequest(
                    "Gaming Laptop", "High end", new BigDecimal("1500.00"),
                    10, 1L, List.of("url1"), "Alienware");
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("POST /admin/products: Validation Error (Negative Price)")
        void shouldRejectNegativePrice() throws Exception {
            // Using a record/DTO with invalid data
            ProductCreateRequest badRequest = new ProductCreateRequest(
                    "Name", "Desc", new BigDecimal("-100"), 10, 1L, List.of(), "Brand");

            mockMvc.perform(post("/admin/products")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest()); // Assumes @Valid is present on controller
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("PUT /admin/products/{id}: Success path")
        void updateProductSuccess() throws Exception {
            mockMvc.perform(put("/admin/products/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk());

            verify(productService).updateProduct(eq(1L), any());
        }
    }

    @Nested
    @DisplayName("Global Exception Mapping")
    class ErrorHandling {
        @Test
        @DisplayName("Handle ResourceNotFoundException -> 404")
        void shouldMapNotFoundException() throws Exception {
            when(productService.getProduct(99L)).thenThrow(new ResourceNotFoundException("Not Found"));

            mockMvc.perform(get("/products/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("Admin Product Torture Tests")
    class AdminTortureTests {

        private final String BASE_URL = "/admin/products";

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("Constraint Violation - Reject negative price and empty name")
        void shouldRejectInvalidProductCreation() throws Exception {
            // Constructing a "hostile" DTO that violates typical constraints
            ProductCreateRequest badRequest = new ProductCreateRequest(
                    "",                // Empty name
                    "Desc",
                    new BigDecimal("-50.00"), // Negative price
                    -1,               // Negative stock
                    1L,
                    List.of(),
                    "Brand"
            );

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

            verifyNoInteractions(productService); // Logic should never reach service
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("Update - Handle Non-Existent ID")
        void shouldReturn404OnUpdateMissingProduct() throws Exception {
            ProductUpdateRequest updateRequest = new ProductUpdateRequest("New Name", "New Desc", BigDecimal.ONE, 5, "Brand", true, 1L);

            when(productService.updateProduct(eq(999L), any()))
                    .thenThrow(new ResourceNotFoundException("Product 999 not found"));

            mockMvc.perform(put(BASE_URL + "/999")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("999")));
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("Delete - Ensure Idempotency or Error Handling")
        void shouldHandleDeleteExecutionFlow() throws Exception {
            // Test behavior when deleting a product that was already deleted or never existed
            doThrow(new ResourceNotFoundException("Already gone"))
                    .when(productService).deleteProduct(888L);

            mockMvc.perform(delete(BASE_URL + "/888").with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER") // Wrong role
        @DisplayName("Security - Verify ROLE_USER cannot reach Admin Delete")
        void shouldBlockStandardUserFromDeletion() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1").with(csrf()))
                    .andExpect(status().isForbidden());

            verify(productService, never()).deleteProduct(anyLong());
        }
    }
}