package com.itc.funkart.product_service.controller;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartItemUpdateDto;
import com.itc.funkart.product_service.dto.response.CartResponse;
import com.itc.funkart.product_service.response.ApiResponse;
import com.itc.funkart.product_service.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API endpoints for managing shopping carts.
 * <p>
 * This controller facilitates the lifecycle of a user's shopping cart,
 * including item addition, quantity adjustments, and the checkout process.
 * </p>
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Shopping Cart", description = "Cart Management API")
public class CartController {

    private final CartService cartService;

    /**
     * Retrieves the current state of a user's shopping cart.
     *
     * @param userId The unique identifier of the user.
     * @return {@link ApiResponse} containing the {@link CartResponse} payload.
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user's shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(new ApiResponse<>(cartService.getCartByUserId(userId)));
    }

    /**
     * Adds a specific quantity of a product to the user's cart.
     *
     * @param userId  The unique identifier of the user.
     * @param request The data containing product ID and quantity.
     * @return Standardized response with the updated cart state.
     */
    @PostMapping("/{userId}/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @PathVariable Long userId,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(cartService.addItemToCart(userId, request), "Item added to cart"));
    }

    /**
     * Removes an entire product entry from the shopping cart.
     *
     * @param userId    The unique identifier of the user.
     * @param productId The ID of the product to be removed.
     * @return Standardized response with the updated cart state.
     */
    @DeleteMapping("/{userId}/items/{productId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItems(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        return ResponseEntity.ok(new ApiResponse<>(cartService.removeItemsFromCart(userId, productId), "Item removed"));
    }

    /**
     * Adjusts the quantity of an existing item in the cart.
     *
     * @param userId    The unique identifier of the user.
     * @param productId The ID of the product to update.
     * @param updateDto The delta change for the quantity.
     * @return Standardized response with the updated cart state.
     */
    @PatchMapping("/{userId}/items/{productId}")
    @Operation(summary = "Update item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @Valid @RequestBody CartItemUpdateDto updateDto) {
        return ResponseEntity.ok(new ApiResponse<>(cartService.updateItemQuantity(userId, productId, updateDto), "Quantity updated"));
    }

    /**
     * Finalizes the cart contents and triggers the order creation workflow.
     * <p>
     * Note: This operation clears the cart upon successful event publication to Kafka.
     * </p>
     *
     * @param userId The unique identifier of the user.
     * @return A confirmation message indicating the order is being processed.
     */
    @PostMapping("/{userId}/checkout")
    @Operation(summary = "Checkout and process order")
    public ResponseEntity<ApiResponse<String>> checkout(@PathVariable Long userId) {
        cartService.checkout(userId);
        return ResponseEntity.ok(new ApiResponse<>("Order processed and cart cleared"));
    }
}