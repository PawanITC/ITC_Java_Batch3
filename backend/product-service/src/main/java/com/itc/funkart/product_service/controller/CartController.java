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
 * REST API for personal shopping cart management.
 * <p>
 * This controller leverages implicit identity resolution. Users do not provide
 * their IDs; the system extracts identity from the authenticated SecurityContext.
 * </p>
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Shopping Cart", description = "Authenticated User Cart Management")
public class CartController {

    private final CartService cartService;

    /**
     * Retrieves the shopping cart for the currently authenticated user.
     *
     * @return {@link ApiResponse} containing the user's cart state.
     */
    @GetMapping("/my-cart")
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        return ResponseEntity.ok(new ApiResponse<>(cartService.getCart()));
    }

    /**
     * Adds a product to the authenticated user's cart.
     *
     * @param request Contains the productId and quantity.
     * @return Updated cart state.
     */
    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(cartService.addItemToCart(request), "Item added to cart"));
    }

    /**
     * Completely removes a product from the authenticated user's cart.
     *
     * @param productId The ID of the product to remove.
     * @return Updated cart state.
     */
    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItems(@PathVariable Long productId) {
        return ResponseEntity.ok(new ApiResponse<>(cartService.removeItemsFromCart(productId), "Item removed"));
    }

    /**
     * Updates the quantity of an item. Resulting quantities <= 0 will remove the item.
     *
     * @param productId The ID of the product to update.
     * @param updateDto The quantity change delta.
     * @return Updated cart state.
     */
    @PatchMapping("/items/{productId}")
    @Operation(summary = "Update item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody CartItemUpdateDto updateDto) {
        return ResponseEntity.ok(new ApiResponse<>(cartService.updateItemQuantity(productId, updateDto), "Quantity updated"));
    }

    /**
     * Finalizes the cart and triggers order processing.
     * <p>
     * Implementation Note: This is a destructive read; the cart is cleared upon
     * successful transition to the order service.
     * </p>
     *
     * @return Success message once the checkout event is published.
     */
    @PostMapping("/checkout")
    @Operation(summary = "Checkout and clear cart")
    public ResponseEntity<ApiResponse<String>> checkout() {
        cartService.checkout();
        return ResponseEntity.ok(new ApiResponse<>("Order processed and cart cleared"));
    }
}