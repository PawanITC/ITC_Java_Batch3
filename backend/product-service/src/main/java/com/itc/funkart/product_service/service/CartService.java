package com.itc.funkart.product_service.service;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartItemUpdateDto;
import com.itc.funkart.product_service.dto.request.CartResponse;
import jakarta.validation.Valid;

public interface CartService {
    CartResponse getCartByUserId(Long userId);
    CartResponse addItemToCart(Long userId, AddToCartRequest request);
    CartResponse removeItemsFromCart(Long userId, Long productId);
    void clearCart(Long userId);
    CartResponse updateItemQuantity(Long userId, Long productId, @Valid CartItemUpdateDto updateDto);
    void checkout(Long userId);
}
