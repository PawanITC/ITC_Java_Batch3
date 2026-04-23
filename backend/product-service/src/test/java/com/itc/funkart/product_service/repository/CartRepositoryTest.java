package com.itc.funkart.product_service.repository;

import com.itc.funkart.product_service.entity.Cart;
import com.itc.funkart.product_service.entity.CartItem;
import com.itc.funkart.product_service.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    private Cart createCart(Long userId) {
        return Cart.builder()
                .userId(userId)
                .build();
    }

    private Product createProduct(String name, String slug) {
        return Product.builder()
                .name(name)
                .slug(slug)
                .price(BigDecimal.valueOf(100))
                .stockQuantity(10)
                .brand("TestBrand")
                .build();
    }

    @Test
    @DisplayName("Should save cart with userId")
    void shouldSaveCart() {
        Cart cart = createCart(1L);

        Cart saved = cartRepository.save(cart);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should enforce unique userId constraint")
    void shouldThrowExceptionWhenDuplicateUserId() {
        Cart cart1 = createCart(1L);
        Cart cart2 = createCart(1L);

        cartRepository.save(cart1);

        assertThatThrownBy(() -> cartRepository.saveAndFlush(cart2))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should find cart by userId")
    void shouldFindCartByUserId() {
        Cart cart = createCart(2L);
        cartRepository.save(cart);

        Optional<Cart> result = cartRepository.findByUserId(2L);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should return empty when userId not found")
    void shouldReturnEmptyWhenUserIdNotFound() {
        Optional<Cart> result = cartRepository.findByUserId(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should persist cart with multiple cart items")
    void shouldSaveCartWithCartItems() {
        Cart cart = cartRepository.save(createCart(3L));

        Product product1 = productRepository.save(createProduct("Phone", "phone-slug"));
        Product product2 = productRepository.save(createProduct("Laptop", "laptop-slug"));

        CartItem item1 = CartItem.builder()
                .cart(cart)
                .product(product1)
                .quantity(2)
                .build();

        CartItem item2 = CartItem.builder()
                .cart(cart)
                .product(product2)
                .quantity(1)
                .build();

        cartItemRepository.save(item1);
        cartItemRepository.saveAndFlush(item2);

        assertThat(cartItemRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Should delete cart by id")
    void shouldDeleteCart() {
        Cart cart = cartRepository.save(createCart(4L));

        cartRepository.deleteById(cart.getId());

        assertThat(cartRepository.findById(cart.getId())).isEmpty();
    }
}

