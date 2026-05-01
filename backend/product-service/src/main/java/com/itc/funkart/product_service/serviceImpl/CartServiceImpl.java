package com.itc.funkart.product_service.serviceImpl;

import com.itc.funkart.product_service.dto.events.OrderEvent;
import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartItemUpdateDto;
import com.itc.funkart.product_service.dto.response.CartResponse;
import com.itc.funkart.product_service.entity.Cart;
import com.itc.funkart.product_service.entity.CartItem;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.enums.OrderEventType;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.mapper.CartMapper;
import com.itc.funkart.product_service.kafka.producer.OrderProducer;
import com.itc.funkart.product_service.repository.CartRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import com.itc.funkart.product_service.service.CartService;
import com.itc.funkart.product_service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Senior-level implementation of {@link CartService}.
 * Centralizes identity resolution and ensures transactional integrity between
 * the RDBMS and Kafka event streams.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderProducer orderProducer;

    /**
     * Private Gatekeeper: DRYs up the identity and retrieval logic.
     * Hits the DB once and maintains the entity in the Hibernate Session.
     */
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
                        () -> cart.getItems().add(CartItem.builder()
                                .cart(cart)
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
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
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

        // Prepare Event Data
        List<Long> productIds = cart.getItems().stream()
                .map(item -> item.getProduct().getId())
                .toList();

        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderEvent event = OrderEvent.builder()
                .eventType(OrderEventType.ORDER_CREATED)
                .userId(cart.getUserId())
                .totalAmount(total)
                .productIds(productIds)
                .build();

        // Database change happens first
        cart.getItems().clear();
        cartRepository.save(cart);

        // Kafka message only fires after the DB COMMIT
        registerOrderEvent(event);
    }

    private void registerOrderEvent(OrderEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    orderProducer.sendOrderEvent(event);
                }
            });
        }
    }
}