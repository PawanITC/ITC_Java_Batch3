package com.itc.funkart.product_service.service;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartResponse;

public interface CartService {
    CartResponse getCartByUserId(Long userId);
    CartResponse addItemToCart(Long userId, AddToCartRequest request);
    CartResponse removeItemFromCart(Long userId, Long productId);
    void clearCart(Long userId);
}
