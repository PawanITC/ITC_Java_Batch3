package com.itc.funkart.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.product.config.SecurityConfig;
import com.itc.funkart.product.dto.request.AddToCartRequest;
import com.itc.funkart.product.dto.request.CartItemUpdateDto;
import com.itc.funkart.product.dto.response.CartResponse;
import com.itc.funkart.product.service.CartService;
import com.itc.funkart.product.service.JwtService;
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
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>CartControllerTest</h2>
 * Validates the REST contract for Shopping Cart management.
 * Focuses on Security Authorization, Input Validation, and JSON structure.
 */
@WebMvcTest(CartController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class})
@DisplayName("Cart Controller - Endpoint & Security Analysis")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("Authenticated Access - Success Branches")
    @WithMockUser(authorities = "ROLE_USER")
    class Authenticated {

        @Test
        @DisplayName("GET /my-cart: Should return 200 and JSON response")
        void getCartSuccess() throws Exception {
            CartResponse response = CartResponse.builder()
                    .cartId(1L).totalAmount(BigDecimal.TEN).items(List.of()).build();

            when(cartService.getCart()).thenReturn(response);

            mockMvc.perform(get("/cart/my-cart"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.cartId").value(1L))
                    .andExpect(jsonPath("$.data.totalAmount").value(10.0));
        }

        @Test
        @DisplayName("PATCH /items/{id}: Should process quantity delta updates")
        void updateQuantitySuccess() throws Exception {
            CartItemUpdateDto updateDto = new CartItemUpdateDto(2); // Record syntax

            mockMvc.perform(patch("/cart/items/500")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Quantity updated"));

            verify(cartService).updateItemQuantity(eq(500L), any());
        }

        @Test
        @DisplayName("DELETE /items/{id}: Should remove item from session cart")
        void deleteItemSuccess() throws Exception {
            mockMvc.perform(delete("/cart/items/500").with(csrf()))
                    .andExpect(status().isOk());

            verify(cartService).removeItemsFromCart(500L);
        }
    }

    @Nested
    @DisplayName("Error & Edge Case Branches")
    class Failures {

        @Test
        @DisplayName("403 Forbidden: Should reject unauthenticated requests to cart")
        void rejectAnonymous() throws Exception {
            mockMvc.perform(get("/cart/my-cart"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("400 Bad Request: Should reject non-positive quantities")
        void rejectInvalidQuantity() throws Exception {
            AddToCartRequest badRequest = AddToCartRequest.builder()
                    .productId(1L).quantity(-5).build();

            mockMvc.perform(post("/cart/items")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }
    }
}