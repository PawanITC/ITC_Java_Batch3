package com.itc.funkart.product_service.service;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartItemUpdateDto;
import com.itc.funkart.product_service.dto.response.CartResponse;
import jakarta.validation.Valid;

/**
 * Service interface for managing shopping cart operations.
 * Handles the logic for persistent user carts, item synchronization,
 * and the transition from cart to order (checkout).
 */
public interface CartService {

    /**
     * Retrieves the cart for a specific user.
     * If no cart exists, one is typically created.
     * * @param userId The unique identifier of the user.
     *
     * @return The current state of the user's cart.
     */
    CartResponse getCartByUserId(Long userId);

    /**
     * Adds a product to the user's cart.
     * If the item already exists, the quantity is incremented.
     * * @param userId The unique identifier of the user.
     *
     * @param request The product ID and quantity to add.
     * @return The updated cart response.
     */
    CartResponse addItemToCart(Long userId, AddToCartRequest request);

    /**
     * Removes all quantities of a specific product from the user's cart.
     * * @param userId The unique identifier of the user.
     *
     * @param productId The ID of the product to remove.
     * @return The updated cart response.
     */
    CartResponse removeItemsFromCart(Long userId, Long productId);

    /**
     * Removes all items from the user's cart.
     * Usually called after a successful order placement.
     * * @param userId The unique identifier of the user.
     */
    void clearCart(Long userId);

    /**
     * Adjusts the quantity of an existing item in the cart.
     * * @param userId The unique identifier of the user.
     *
     * @param productId The ID of the product to update.
     * @param updateDto The quantity change (positive or negative).
     * @return The updated cart response.
     */
    CartResponse updateItemQuantity(Long userId, Long productId, @Valid CartItemUpdateDto updateDto);

    /**
     * Initiates the checkout process.
     * Calculates totals and publishes an OrderEvent to Kafka for the Order Service.
     * * @param userId The unique identifier of the user performing the checkout.
     *
     * @throws IllegalStateException if the cart is empty.
     */
    void checkout(Long userId);
}