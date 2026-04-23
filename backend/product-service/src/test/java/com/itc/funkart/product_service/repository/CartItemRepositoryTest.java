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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CartItemRepositoryTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

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
    @DisplayName("Should save cart item with cart and product")
    void shouldSaveCartItem() {
        Cart cart = cartRepository.save(createCart(1L));
        Product product = productRepository.save(createProduct("Phone", "phone-slug"));

        CartItem item = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(2)
                .build();

        CartItem saved = cartItemRepository.save(item);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCart()).isNotNull();
        assertThat(saved.getProduct()).isNotNull();
        assertThat(saved.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should persist multiple cart items for one cart")
    void shouldSaveMultipleItemsForCart() {
        Cart cart = cartRepository.save(createCart(2L));
        Product product1 = productRepository.save(createProduct("Phone", "phone-slug"));
        Product product2 = productRepository.save(createProduct("Laptop", "laptop-slug"));

        CartItem item1 = CartItem.builder()
                .cart(cart)
                .product(product1)
                .quantity(1)
                .build();

        CartItem item2 = CartItem.builder()
                .cart(cart)
                .product(product2)
                .quantity(3)
                .build();

        cartItemRepository.save(item1);
        cartItemRepository.save(item2);

        assertThat(cartItemRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Should allow same product in different carts")
    void shouldAllowSameProductInDifferentCarts() {
        Cart cart1 = cartRepository.save(createCart(3L));
        Cart cart2 = cartRepository.save(createCart(4L));
        Product product = productRepository.save(createProduct("Item", "item-slug"));

        CartItem item1 = CartItem.builder()
                .cart(cart1)
                .product(product)
                .quantity(2)
                .build();

        CartItem item2 = CartItem.builder()
                .cart(cart2)
                .product(product)
                .quantity(5)
                .build();

        cartItemRepository.save(item1);
        cartItemRepository.save(item2);

        assertThat(cartItemRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Should update cart item quantity")
    void shouldUpdateCartItemQuantity() {
        Cart cart = cartRepository.save(createCart(5L));
        Product product = productRepository.save(createProduct("Book", "book-slug"));

        CartItem item = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(1)
                .build();

        CartItem saved = cartItemRepository.save(item);

        saved.setQuantity(10);
        CartItem updated = cartItemRepository.save(saved);

        assertThat(updated.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should delete cart item")
    void shouldDeleteCartItem() {
        Cart cart = cartRepository.save(createCart(6L));
        Product product = productRepository.save(createProduct("Gadget", "gadget-slug"));

        CartItem item = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(1)
                .build();

        CartItem saved = cartItemRepository.save(item);

        cartItemRepository.deleteById(saved.getId());

        assertThat(cartItemRepository.findAll()).isEmpty();
    }
}

