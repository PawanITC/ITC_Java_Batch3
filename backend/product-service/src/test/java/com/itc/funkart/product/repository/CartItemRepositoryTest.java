package com.itc.funkart.product.repository;

import com.itc.funkart.product.entity.Cart;
import com.itc.funkart.product.entity.CartItem;
import com.itc.funkart.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CartItemRepositoryTest</h2>
 * <p>
 * Tests the persistence layer for cart items.
 * Uses {@link TestEntityManager} for cleaner state management between tests.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Cart Item Repository Integration Tests")
class CartItemRepositoryTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    // --- Helpers using Entity Builders ---

    private Cart createCart(Long userId) {
        return Cart.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .build();
    }

    private Product createProduct(String name, String slug) {
        return Product.builder()
                .name(name)
                .slug(slug)
                .price(BigDecimal.valueOf(100))
                .stockQuantity(10)
                .brand("TestBrand")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Save - Should persist item and link to cart/product")
    void shouldSaveCartItem() {
        // Arrange
        Cart cart = entityManager.persist(createCart(1L));
        Product product = entityManager.persist(createProduct("Phone", "phone-slug"));

        CartItem item = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(2)
                .build();

        // Act
        CartItem saved = cartItemRepository.save(item);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCart().getUserId()).isEqualTo(1L);
        assertThat(saved.getProduct().getName()).isEqualTo("Phone");
        assertThat(saved.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Query - Should find all items belonging to a specific cart")
    void shouldPersistMultipleItemsForOneCart() {
        // Arrange
        Cart cart = entityManager.persist(createCart(2L));
        Product p1 = entityManager.persist(createProduct("Item 1", "slug-1"));
        Product p2 = entityManager.persist(createProduct("Item 2", "slug-2"));

        cartItemRepository.save(CartItem.builder().cart(cart).product(p1).quantity(1).build());
        cartItemRepository.save(CartItem.builder().cart(cart).product(p2).quantity(5).build());

        entityManager.flush();
        entityManager.clear();

        // Act & Assert
        assertThat(cartItemRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Update - Should modify quantity in place")
    void shouldUpdateCartItemQuantity() {
        // Arrange
        Cart cart = entityManager.persist(createCart(3L));
        Product product = entityManager.persist(createProduct("Book", "book-slug"));
        CartItem item = cartItemRepository.save(CartItem.builder().cart(cart).product(product).quantity(1).build());

        // Act
        item.setQuantity(10);
        CartItem updated = cartItemRepository.saveAndFlush(item);

        // Assert
        assertThat(updated.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Delete - Should remove item without affecting the Product (Branch: Referential Integrity)")
    void shouldDeleteCartItemButKeepProduct() {
        // Arrange
        Cart cart = entityManager.persist(createCart(4L));
        Product product = entityManager.persist(createProduct("Keep Me", "keep-me"));
        CartItem item = cartItemRepository.save(CartItem.builder().cart(cart).product(product).quantity(1).build());

        // Act
        cartItemRepository.delete(item);
        entityManager.flush();

        // Assert
        assertThat(cartItemRepository.findById(item.getId())).isEmpty();
        assertThat(entityManager.find(Product.class, product.getId())).isNotNull();
    }

    @Test
    @DisplayName("Unique Constraint Branch - Verify item can exist in multiple user carts")
    void shouldAllowSameProductInDifferentCarts() {
        // Arrange
        Cart cartA = entityManager.persist(createCart(10L));
        Cart cartB = entityManager.persist(createCart(11L));
        Product product = entityManager.persist(createProduct("Shared", "shared"));

        // Act
        cartItemRepository.save(CartItem.builder().cart(cartA).product(product).quantity(1).build());
        cartItemRepository.save(CartItem.builder().cart(cartB).product(product).quantity(1).build());

        // Assert
        assertThat(cartItemRepository.findAll()).hasSize(2);
    }
}