package com.itc.funkart.product_service.controller;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartResponse;
import com.itc.funkart.product_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponse> addItem(@PathVariable Long userId, @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItemToCart(userId, request));
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long userId, @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItemFromCart(userId, productId));
    }
}
