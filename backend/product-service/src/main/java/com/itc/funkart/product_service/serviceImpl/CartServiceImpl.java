package com.itc.funkart.product_service.serviceImpl;


import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartResponse;
import com.itc.funkart.product_service.entity.Cart;
import com.itc.funkart.product_service.entity.CartItem;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.mapper.CartMapper;
import com.itc.funkart.product_service.repository.CartRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import com.itc.funkart.product_service.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartByUserId(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check if product already in cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + request.getQuantity());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
        }

        return CartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItemFromCart(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        return CartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    // Helper method to ensure every user has a cart
    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .userId(userId)
                        .build()));
    }
}
