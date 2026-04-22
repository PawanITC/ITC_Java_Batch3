package com.itc.funkart.product_service.mapper;

import com.itc.funkart.product_service.dto.request.CartItemResponse;
import com.itc.funkart.product_service.dto.request.CartResponse;
import com.itc.funkart.product_service.entity.Cart;
import com.itc.funkart.product_service.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class CartMapper {

    public static CartResponse toResponse(Cart cart) {
        if (cart == null) return null;

        List<CartItemResponse> itemDtos = cart.getItems().stream()
                .map(CartMapper::toItemResponse)
                .collect(Collectors.toList());

        BigDecimal total = itemDtos.stream()
                .map(CartItemResponse::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .items(itemDtos)
                .totalAmount(total)
                .build();
    }

    private static CartItemResponse toItemResponse(CartItem item) {
        if (item.getProduct() == null) {
            throw new IllegalStateException("CartItem must have a valid Product reference");
        }

        BigDecimal price = item.getProduct().getPrice();
        Integer qty = item.getQuantity();

        return CartItemResponse.builder()
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .price(price)
                .quantity(qty)
                .subTotal(price.multiply(BigDecimal.valueOf(qty)))
                .build();
    }
}
