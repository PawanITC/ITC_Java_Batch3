package com.itc.funkart.product.controller;

import com.itc.funkart.common.dto.response.ApiResponse;
import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.product.dto.request.AddToCartRequest;
import com.itc.funkart.product.dto.request.CartItemUpdateDto;
import com.itc.funkart.product.dto.response.CartResponse;
import com.itc.funkart.product.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>CartController</h2>
 * <p>
 * REST API for personal shopping cart management.
 * </p>
 * <p>
 * This controller leverages implicit identity resolution. User IDs are extracted
 * from the authenticated SecurityContext, preventing ID spoofing and ensuring
 * that users can only modify their own data.
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
     * @return {@link ResponseEntity} containing an {@link ApiResponse} with the {@link CartResponse}.
     */
    @GetMapping("/my-cart")
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(), "Cart retrieved successfully"));
    }

    /**
     * Adds a product to the authenticated user's cart.
     *
     * @param request Contains the productId and initial quantity.
     * @return {@link ResponseEntity} containing the updated {@link CartResponse}.
     */
    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cartService.addItemToCart(request), "Item added to cart"));
    }

    /**
     * Completely removes a product from the authenticated user's cart.
     *
     * @param productId The ID of the product to remove.
     * @return {@link ResponseEntity} containing the updated {@link CartResponse}.
     */
    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItems(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.removeItemsFromCart(productId), "Item removed"));
    }

    /**
     * Updates the quantity of an existing item in the cart.
     * Resulting quantities <= 0 will result in item removal.
     *
     * @param productId The ID of the product to update.
     * @param updateDto The new target quantity.
     * @return {@link ResponseEntity} containing the updated {@link CartResponse}.
     */
    @PatchMapping("/items/{productId}")
    @Operation(summary = "Update item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody CartItemUpdateDto updateDto) {
        return ResponseEntity.ok(ApiResponse.success(cartService.updateItemQuantity(productId, updateDto), "Quantity updated"));
    }

    /**
     * Finalizes the current cart and initiates the order processing workflow.
     * <p>
     * <b>Note:</b> This operation is destructive; the cart state is purged upon
     * successful event publication to the Order Service.
     * </p>
     *
     * @return {@link ResponseEntity} with a success message confirming checkout.
     */
    @PostMapping("/checkout")
    @Operation(summary = "Checkout and clear cart")
    public ResponseEntity<ApiResponse<String>> checkout(
            @AuthenticationPrincipal JwtUserDto user) {
        cartService.checkout(user != null ? user.email() : null);
        return ResponseEntity.ok(ApiResponse.success("Order processed and cart cleared", "Checkout successful"));
    }
}