package com.itc.funkart.product_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.product_service.config.SecurityConfig;
import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.response.CartItemResponse;
import com.itc.funkart.product_service.dto.response.CartResponse;
import com.itc.funkart.product_service.exceptions.EmptyCartException;
import com.itc.funkart.product_service.service.CartService;
import com.itc.funkart.product_service.service.JwtService;
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

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>CartControllerTest</h2>
 * Validates cart operations including item management and checkout logic.
 * Ensures Role-Based Access Control (RBAC) is enforced for cart endpoints.
 */
@WebMvcTest(CartController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class})
@DisplayName("Cart Controller Unit Tests")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtService jwtService;

    /**
     * Inline MockitoBean to satisfy Security auto-configuration requirements
     * and suppress generated security password logs.
     */
    @MockitoBean
    private UserDetailsService userDetailsService;

    // --- GET CART TESTS ---

    /**
     * Tests successful retrieval of a user's cart.
     * Validates that the data is correctly wrapped in the ApiResponse 'data' field.
     */
    @Test
    @WithMockUser(authorities = "ROLE_USER")
    @DisplayName("GET /cart/{userId} - Happy Path")
    void shouldGetCartSuccessfully() throws Exception {
        CartResponse response = createCartResponse(1L, 10L, List.of());
        when(cartService.getCartByUserId(10L)).thenReturn(response);

        mockMvc.perform(get("/cart/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(10L))
                .andExpect(jsonPath("$.message").value("Operation successful"));
    }

    // --- ADD TO CART TESTS ---

    /**
     * Verifies that the @Valid annotations on the DTO trigger a 400 Bad Request
     * when constraints (like minimum quantity) are violated.
     */
    @Test
    @WithMockUser(authorities = "ROLE_USER")
    @DisplayName("POST /cart/{userId}/items - Validation Failure (Invalid Qty)")
    void shouldReturn400WhenQuantityIsZero() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .productId(1L)
                .quantity(0) // Should fail @Min(1)
                .build();

        mockMvc.perform(post("/cart/1/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- CHECKOUT TESTS ---

    /**
     * Tests successful checkout flow.
     */
    @Test
    @WithMockUser(authorities = "ROLE_USER")
    @DisplayName("POST /cart/{userId}/checkout - Success")
    void shouldCheckoutSuccessfully() throws Exception {
        mockMvc.perform(post("/cart/1/checkout").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Operation successful"));

        verify(cartService, times(1)).checkout(1L);
    }

    /**
     * Validates business logic error handling when attempting to checkout an empty cart.
     */
    @Test
    @WithMockUser(authorities = "ROLE_USER")
    @DisplayName("POST /cart/{userId}/checkout - Failure (Branch: Empty Cart)")
    void shouldReturn400WhenCheckoutEmptyCart() throws Exception {
        // Throw your specific business exception
        doThrow(new EmptyCartException("Cart is empty")).when(cartService).checkout(1L);

        mockMvc.perform(post("/cart/1/checkout").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Cart is empty"))
                .andExpect(jsonPath("$.error.code").value("EMPTY_CART"));
    }

    // --- SECURITY TESTS ---

    /**
     * Ensures that cart endpoints are protected from unauthenticated access.
     */
    @Test
    @DisplayName("GET /cart/{userId} - Unauthorized")
    void shouldDenyAnonymousAccessToCart() throws Exception {
        mockMvc.perform(get("/cart/1"))
                .andExpect(status().isForbidden());
    }

    // --- Helpers using Builders ---
    private CartResponse createCartResponse(Long id, Long userId, List<CartItemResponse> items) {
        return CartResponse.builder()
                .cartId(id)
                .userId(userId)
                .items(items)
                .totalAmount(BigDecimal.ZERO)
                .build();
    }
}