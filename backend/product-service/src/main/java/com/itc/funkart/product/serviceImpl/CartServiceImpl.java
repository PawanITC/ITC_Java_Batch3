package com.itc.funkart.product.serviceImpl;

import com.itc.funkart.common.dto.event.checkout.CheckoutInitiatedEvent;
import com.itc.funkart.common.dto.event.checkout.CheckoutItemPayload;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.product.dto.request.AddToCartRequest;
import com.itc.funkart.product.dto.request.CartItemUpdateDto;
import com.itc.funkart.product.dto.response.CartResponse;
import com.itc.funkart.product.entity.Cart;
import com.itc.funkart.product.entity.CartItem;
import com.itc.funkart.product.entity.Product;
import com.itc.funkart.product.exceptions.ResourceNotFoundException;
import com.itc.funkart.product.kafka.producer.CheckoutProducer;
import com.itc.funkart.product.mapper.CartMapper;
import com.itc.funkart.product.repository.CartRepository;
import com.itc.funkart.product.repository.ProductRepository;
import com.itc.funkart.product.service.CartService;
import com.itc.funkart.product.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CheckoutProducer checkoutProducer;

    private Cart getAuthenticatedCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder()
                                .userId(userId)
                                .items(new ArrayList<>())
                                .build()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        return CartMapper.toResponse(getAuthenticatedCart());
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(AddToCartRequest request) {
        Cart cart = getAuthenticatedCart();

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .ifPresentOrElse(
                        item -> item.setQuantity(item.getQuantity() + request.quantity()),
                        () -> cart.addCartItem(CartItem.builder()
                                .product(product)
                                .quantity(request.quantity())
                                .build())
                );

        return CartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItemsFromCart(Long productId) {
        Cart cart = getAuthenticatedCart();

        cart.getItems().removeIf(item ->
                item.getProduct().getId().equals(productId)
        );

        return CartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void clearCart() {
        Cart cart = getAuthenticatedCart();
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long productId, CartItemUpdateDto updateDto) {
        Cart cart = getAuthenticatedCart();

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not in cart"));

        int newQuantity = item.getQuantity() + updateDto.quantityChange();

        if (newQuantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(newQuantity);
        }

        return CartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void checkout() {
        Cart cart = getAuthenticatedCart();

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }

        List<CheckoutItemPayload> items = cart.getItems().stream()
                .map(ci -> {
                    BigDecimal price = ci.getProduct().getPrice();
                    int qty = ci.getQuantity();

                    return new CheckoutItemPayload(
                            ci.getProduct().getId(),
                            qty,
                            price,
                            price.multiply(BigDecimal.valueOf(qty))
                    );
                })
                .toList();

        BigDecimal total = items.stream()
                .map(CheckoutItemPayload::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CheckoutInitiatedEvent event = CheckoutInitiatedEvent.builder()
                .eventType(OrderEventType.ORDER_INITIATED)
                .customerId(cart.getUserId())
                .totalAmount(total)
                .items(items)
                .currency("USD")
                .build();

        log.debug("🛒 Checkout initiated | user={} | total={}", cart.getUserId(), total);

        // MVP RULE:
        // ONLY publish event — DO NOT mutate cart state here.
        checkoutProducer.sendCheckoutEvent(event);
    }
}