package com.itc.funkart.product.mapper;

import com.itc.funkart.product.dto.response.CartItemResponse;
import com.itc.funkart.product.dto.response.CartResponse;
import com.itc.funkart.product.entity.Cart;
import com.itc.funkart.product.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;

/**
 * <h2>CartMapper</h2>
 * <p>
 * Logic for transforming complex Cart aggregates into flattened responses.
 * Calculates totals and sub-totals during the mapping process.
 * </p>
 */
public class CartMapper {

    /**
     * Transforms a {@link Cart} entity into a comprehensive {@link CartResponse}.
     * Calculates the total cart amount based on item sub-totals.
     *
     * @param cart the user's persistent cart entity.
     * @return a populated cart response or null if the input is null.
     */
    public static CartResponse toResponse(Cart cart) {
        if (cart == null) return null;

        List<CartItemResponse> itemDtos = cart.getItems().stream()
                .map(CartMapper::toItemResponse)
                .toList();

        BigDecimal total = itemDtos.stream()
                .map(CartItemResponse::subTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .items(itemDtos)
                .totalAmount(total)
                .build();
    }

    /**
     * Helper to map an individual {@link CartItem} to a {@link CartItemResponse}.
     */
    private static CartItemResponse toItemResponse(CartItem item) {
        BigDecimal price = item.getProduct().getPrice();
        int qty = item.getQuantity();

        String imageUrl = item.getProduct().getImages() != null
                && !item.getProduct().getImages().isEmpty()
                ? item.getProduct().getImages().get(0).getImageUrl()
                : null;

        return CartItemResponse.builder()
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .imageUrl(imageUrl)
                .price(price)
                .quantity(qty)
                .subTotal(price.multiply(BigDecimal.valueOf(qty)))
                .build();
    }
}