package com.itc.funkart.product.service;

import com.itc.funkart.product.dto.request.AddToCartRequest;
import com.itc.funkart.product.dto.request.CartItemUpdateDto;
import com.itc.funkart.product.dto.response.CartResponse;
import com.itc.funkart.product.exceptions.ResourceNotFoundException;

/**
 * Service interface for managing shopping cart operations.
 * <p>
 * <b>Security Note:</b> All methods resolve user identity internally via the
 * {@code SecurityContextHolder}. Calls to these methods must occur within
 * the context of an authenticated web request.
 * </p>
 */
public interface CartService {

    /**
     * Retrieves the cart for the currently authenticated user.
     *
     * @return The current state of the user's cart.
     * @throws IllegalStateException if no authenticated user is found in context.
     */
    CartResponse getCart();

    /**
     * Adds a product to the authenticated user's cart.
     *
     * @param request The product ID and quantity to add.
     * @return The updated cart response.
     * @throws ResourceNotFoundException if the productId does not exist.
     */
    CartResponse addItemToCart(AddToCartRequest request);

    /**
     * Removes all quantities of a specific product from the authenticated user's cart.
     *
     * @param productId The ID of the product to remove.
     * @return The updated cart response.
     */
    CartResponse removeItemsFromCart(Long productId);

    /**
     * Removes all items from the authenticated user's cart.
     */
    void clearCart();

    /**
     * Adjusts the quantity of an existing item in the authenticated user's cart.
     *
     * @param productId The ID of the product to update.
     * @param updateDto The quantity change (positive or negative).
     * @return The updated cart response.
     * @throws ResourceNotFoundException if the item is not currently in the cart.
     */
    CartResponse updateItemQuantity(Long productId, CartItemUpdateDto updateDto);

    /**
     * Initiates the checkout process for the authenticated user.
     * <p>
     * This triggers the conversion of cart items into an Order event.
     * </p>
     *
     * @throws IllegalStateException if the cart is empty or user is unauthenticated.
     */
    void checkout();
}