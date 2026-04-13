package com.itc.funkart.product_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartItemResponse;
import com.itc.funkart.product_service.dto.request.CartItemUpdateDto;
import com.itc.funkart.product_service.dto.request.CartResponse;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.service.CartService;
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

@WebMvcTest(CartController.class)
@DisplayName("CartController Tests")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    private CartResponse createCartResponse(Long cartId, Long userId, List<CartItemResponse> items) {
        return CartResponse.builder()
                .cartId(cartId)
                .userId(userId)
                .items(items)
                .totalAmount(BigDecimal.valueOf(0))
                .build();
    }

    private CartItemResponse createCartItemResponse(Long productId, String productName, Integer quantity) {
        return CartItemResponse.builder()
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .price(BigDecimal.valueOf(99.99))
                .build();
    }

    @Test
    @DisplayName("Should get cart by user id successfully")
    void shouldGetCartByUserIdSuccessfully() throws Exception {
        // Arrange
        CartItemResponse item = createCartItemResponse(1L, "Laptop", 2);
        CartResponse cart = createCartResponse(1L, 1L, List.of(item));

        when(cartService.getCartByUserId(1L)).thenReturn(cart);

        // Act & Assert
        mockMvc.perform(get("/api/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.items[0].productName").value("Laptop"));

        verify(cartService, times(1)).getCartByUserId(1L);
    }

    @Test
    @DisplayName("Should return empty cart when user has no items")
    void shouldReturnEmptyCartWhenUserHasNoItems() throws Exception {
        // Arrange
        CartResponse cart = createCartResponse(1L, 1L, List.of());

        when(cartService.getCartByUserId(1L)).thenReturn(cart);

        // Act & Assert
        mockMvc.perform(get("/api/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        verify(cartService, times(1)).getCartByUserId(1L);
    }

    @Test
    @DisplayName("Should add item to cart successfully")
    void shouldAddItemToCartSuccessfully() throws Exception {
        // Arrange
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        CartItemResponse item = createCartItemResponse(1L, "Laptop", 2);
        CartResponse cart = createCartResponse(1L, 1L, List.of(item));

        when(cartService.addItemToCart(1L, request)).thenReturn(cart);

        // Act & Assert
        mockMvc.perform(post("/api/cart/1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        verify(cartService, times(1)).addItemToCart(1L, request);
    }

    @Test
    @DisplayName("Should return 404 when trying to add invalid product")
    void shouldReturn404WhenTryingToAddInvalidProduct() throws Exception {
        // Arrange
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(999L);
        request.setQuantity(1);

        when(cartService.addItemToCart(anyLong(), any(AddToCartRequest.class)))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        // Act & Assert
        mockMvc.perform(post("/api/cart/1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(cartService, times(1)).addItemToCart(1L, request);
    }

    @Test
    @DisplayName("Should remove item from cart successfully")
    void shouldRemoveItemFromCartSuccessfully() throws Exception {
        // Arrange
        CartResponse cart = createCartResponse(1L, 1L, List.of());

        when(cartService.removeItemsFromCart(1L, 1L)).thenReturn(cart);

        // Act & Assert
        mockMvc.perform(delete("/api/cart/1/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        verify(cartService, times(1)).removeItemsFromCart(1L, 1L);
    }

    @Test
    @DisplayName("Should update item quantity successfully")
    void shouldUpdateItemQuantitySuccessfully() throws Exception {
        // Arrange
        CartItemUpdateDto updateDto = new CartItemUpdateDto();
        updateDto.setQuantityChange(3);

        CartItemResponse item = createCartItemResponse(1L, "Laptop", 5);
        CartResponse cart = createCartResponse(1L, 1L, List.of(item));

        when(cartService.updateItemQuantity(1L, 1L, updateDto)).thenReturn(cart);

        // Act & Assert
        mockMvc.perform(patch("/api/cart/1/items/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5));

        verify(cartService, times(1)).updateItemQuantity(1L, 1L, updateDto);
    }

    @Test
    @DisplayName("Should checkout successfully")
    void shouldCheckoutSuccessfully() throws Exception {
        // Arrange
        doNothing().when(cartService).checkout(1L);

        // Act & Assert
        mockMvc.perform(post("/api/cart/1/checkout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Order processed and cart cleared"));

        verify(cartService, times(1)).checkout(1L);
    }

    @Test
    @DisplayName("Should return 404 when checkout cart not found")
    void shouldReturn404WhenCheckoutCartNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Cart not found"))
                .when(cartService).checkout(anyLong());

        // Act & Assert
        mockMvc.perform(post("/api/cart/999/checkout"))
                .andExpect(status().isNotFound());

        verify(cartService, times(1)).checkout(999L);
    }

    @Test
    @DisplayName("Should return 500 when checkout empty cart")
    void shouldReturn500WhenCheckoutEmptyCart() throws Exception {
        // Arrange
        doThrow(new IllegalStateException("Cannot checkout an empty cart"))
                .when(cartService).checkout(anyLong());

        // Act & Assert
        mockMvc.perform(post("/api/cart/1/checkout"))
                .andExpect(status().isInternalServerError());

        verify(cartService, times(1)).checkout(1L);
    }

    @Test
    @DisplayName("Should return correct content type for cart")
    void shouldReturnCorrectContentTypeForCart() throws Exception {
        // Arrange
        CartResponse cart = createCartResponse(1L, 1L, List.of());
        when(cartService.getCartByUserId(1L)).thenReturn(cart);

        // Act & Assert
        mockMvc.perform(get("/api/cart/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));

        verify(cartService, times(1)).getCartByUserId(1L);
    }

    @Test
    @DisplayName("Should handle multiple items in cart")
    void shouldHandleMultipleItemsInCart() throws Exception {
        // Arrange
        CartItemResponse item1 = createCartItemResponse(1L, "Laptop", 2);
        CartItemResponse item2 = createCartItemResponse(2L, "Phone", 1);
        CartResponse cart = createCartResponse(1L, 1L, List.of(item1, item2));

        when(cartService.getCartByUserId(1L)).thenReturn(cart);

        // Act & Assert
        mockMvc.perform(get("/api/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.items[1].productName").value("Phone"));

        verify(cartService, times(1)).getCartByUserId(1L);
    }

    @Test
    @DisplayName("Should validate request body for add to cart")
    void shouldValidateRequestBodyForAddToCart() throws Exception {
        // Arrange
        String invalidRequest = "{}"; // Missing required fields

        // Act & Assert
        mockMvc.perform(post("/api/cart/1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).addItemToCart(anyLong(), any());
    }

    @Test
    @DisplayName("Should handle delete request for removing cart item")
    void shouldHandleDeleteRequestForRemovingCartItem() throws Exception {
        // Arrange
        CartResponse cart = createCartResponse(1L, 1L, List.of());
        when(cartService.removeItemsFromCart(1L, 2L)).thenReturn(cart);

        // Act & Assert
        mockMvc.perform(delete("/api/cart/1/items/2"))
                .andExpect(status().isOk());

        verify(cartService, times(1)).removeItemsFromCart(1L, 2L);
    }

    @Test
    @DisplayName("Should return valid cart response structure")
    void shouldReturnValidCartResponseStructure() throws Exception {
        // Arrange
        CartItemResponse item = createCartItemResponse(1L, "Laptop", 2);
        CartResponse cart = createCartResponse(1L, 1L, List.of(item));

        when(cartService.getCartByUserId(1L)).thenReturn(cart);

        // Act & Assert
        mockMvc.perform(get("/api/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").exists())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.items").exists())
                .andExpect(jsonPath("$.totalAmount").exists());

        verify(cartService, times(1)).getCartByUserId(1L);
    }
}

