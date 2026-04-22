package com.itc.funkart.product_service.serviceImpl;


import com.itc.funkart.product_service.dto.events.OrderEvent;
import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartItemUpdateDto;
import com.itc.funkart.product_service.dto.request.CartResponse;
import com.itc.funkart.product_service.entity.Cart;
import com.itc.funkart.product_service.entity.CartItem;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.mapper.CartMapper;
import com.itc.funkart.product_service.producer.OrderProducer;
import com.itc.funkart.product_service.repository.CartRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import com.itc.funkart.product_service.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderProducer orderProducer;

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
    public CartResponse removeItemsFromCart(Long userId, Long productId) {
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

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long productId, CartItemUpdateDto updateDto) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not in cart"));

        int newQuantity = item.getQuantity() + updateDto.getQuantityChange();

        if (newQuantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(newQuantity);
        }

        return CartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void checkout(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }

        List<Long> productIds = cart.getItems().stream()
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toList());

        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderEvent event = OrderEvent.builder()
                .eventType("ORDER_CREATED")
                .userId(userId)
                .totalAmount(total)
                .productIds(productIds)
                .build();

        cart.getItems().clear();
        cartRepository.save(cart);

        //orderProducer.sendOrderEvent(event);
    }

    // Helper method to ensure every user has a cart
    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder()
                                .userId(userId)
                                .items(new ArrayList<>()) // ✅ fix
                                .build()
                ));
    }
}
