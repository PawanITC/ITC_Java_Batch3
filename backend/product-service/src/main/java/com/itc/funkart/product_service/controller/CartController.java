package com.itc.funkart.product_service.controller;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartItemUpdateDto;
import com.itc.funkart.product_service.dto.request.CartResponse;
import com.itc.funkart.product_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * Cart Controller
 * 
 * REST API endpoints for managing shopping carts.
 * Provides operations for viewing, adding, removing, and updating items in a user's shopping cart.
 * Handles cart checkout and order processing.
 * 
 * Base URL: /api/cart
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(
    name = "Shopping Cart",
    description = "Shopping Cart Management API - Add, remove, and manage items in the shopping cart"
)
public class CartController {

    private final CartService cartService;

    /**
     * Retrieve user's shopping cart
     * 
     * Fetches the current shopping cart for a specific user including:
     * - All items in the cart
     * - Item quantities
     * - Product details for each item
     * - Cart total value
     * 
     * @param userId The unique user identifier
     * @return User's shopping cart with all items and details
     */
    @GetMapping("/{userId}")
    @Operation(
        summary = "Get user's shopping cart",
        description = "Retrieve the complete shopping cart for a specific user, including all items, " +
                      "quantities, product details, and cart totals.",
        tags = {"Shopping Cart", "Retrieve"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Shopping cart retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cart not found for the given user ID"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<CartResponse> getCart(
        @Parameter(
            name = "userId",
            description = "The unique identifier of the user whose cart to retrieve",
            required = true,
            example = "1"
        )
        @PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    /**
     * Add item to shopping cart
     * 
     * Adds a product to the user's shopping cart. If the product is already in the cart,
     * the quantity is updated instead of adding a duplicate entry.
     * 
     * Request Body Example:
     * {
     *   "productId": 1,
     *   "quantity": 2
     * }
     * 
     * @param userId The unique user identifier
     * @param request Details of the item to add (productId and quantity)
     * @return Updated shopping cart
     */
    @PostMapping("/{userId}/items")
    @Operation(
        summary = "Add item to cart",
        description = "Add a product to the user's shopping cart. If the product already exists in the cart, " +
                      "the quantity is incremented. Validates product existence and availability.",
        tags = {"Shopping Cart", "Create"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Item added to cart successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - missing or invalid product ID or quantity"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found or user not found"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<CartResponse> addItem(
        @Parameter(
            name = "userId",
            description = "The unique identifier of the user adding the item",
            required = true,
            example = "1"
        )
        @PathVariable Long userId,
        @Parameter(
            description = "Details of the item to add to cart",
            required = true
        )
        @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItemToCart(userId, request));
    }

    /**
     * Remove item from shopping cart
     * 
     * Removes a specific product and all its quantity from the user's shopping cart.
     * 
     * @param userId The unique user identifier
     * @param productId The unique product identifier to remove from cart
     * @return Updated shopping cart
     */
    @DeleteMapping("/{userId}/items/{productId}")
    @Operation(
        summary = "Remove item from cart",
        description = "Remove a specific product from the user's shopping cart. Deletes all quantities " +
                      "of the product from the cart.",
        tags = {"Shopping Cart", "Delete"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Item removed from cart successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cart or product not found in cart"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<CartResponse> removeItems(
        @Parameter(
            name = "userId",
            description = "The unique identifier of the user removing the item",
            required = true,
            example = "1"
        )
        @PathVariable Long userId,
        @Parameter(
            name = "productId",
            description = "The unique identifier of the product to remove from cart",
            required = true,
            example = "5"
        )
        @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItemsFromCart(userId, productId));
    }

    /**
     * Update item quantity in cart
     * 
     * Modifies the quantity of a specific product in the user's shopping cart.
     * Validates that the new quantity is within acceptable limits.
     * 
     * Request Body Example:
     * {
     *   "quantity": 5
     * }
     * 
     * @param userId The unique user identifier
     * @param productId The unique product identifier
     * @param updateDto The new quantity for the product
     * @return Updated shopping cart
     */
    @PatchMapping("/{userId}/items/{productId}")
    @Operation(
        summary = "Update item quantity in cart",
        description = "Update the quantity of a specific product in the user's shopping cart. " +
                      "Validates quantity limits and product availability.",
        tags = {"Shopping Cart", "Update"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Item quantity updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid quantity value (must be greater than 0)"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cart or product not found in cart"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<CartResponse> updateItemQuantity(
        @Parameter(
            name = "userId",
            description = "The unique identifier of the user updating the item",
            required = true,
            example = "1"
        )
        @PathVariable Long userId,
        @Parameter(
            name = "productId",
            description = "The unique identifier of the product to update",
            required = true,
            example = "5"
        )
        @PathVariable Long productId,
        @Parameter(
            description = "New quantity for the product",
            required = true
        )
        @Valid @RequestBody CartItemUpdateDto updateDto) {
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, productId, updateDto));
    }

    /**
     * Checkout and process order
     * 
     * Processes the checkout for a user's shopping cart:
     * - Validates cart contents
     * - Processes the order
     * - Clears the cart after successful checkout
     * - Publishes order event to Kafka
     * 
     * @param userId The unique user identifier
     * @return Checkout confirmation message
     */
    @PostMapping("/{userId}/checkout")
    @Operation(
        summary = "Checkout and process order",
        description = "Process the checkout for a user's shopping cart. Validates cart contents, " +
                      "processes the order, publishes order event to Kafka, and clears the cart.",
        tags = {"Shopping Cart", "Checkout"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Order processed successfully and cart cleared"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Cart is empty or validation failed"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User or cart not found"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error or payment processing failure"
        )
    })
    public ResponseEntity<String> checkout(
        @Parameter(
            name = "userId",
            description = "The unique identifier of the user checking out",
            required = true,
            example = "1"
        )
        @PathVariable Long userId) {
        cartService.checkout(userId);
        return ResponseEntity.ok("Order processed and cart cleared");
    }
}
