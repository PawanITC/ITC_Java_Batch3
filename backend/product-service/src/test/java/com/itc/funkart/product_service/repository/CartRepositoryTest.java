package com.itc.funkart.product_service.repository;

import com.itc.funkart.product_service.entity.Cart;
import com.itc.funkart.product_service.entity.CartItem;
import com.itc.funkart.product_service.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>CartRepositoryTest</h2>
 * <p>
 * Tests Cart persistence and relationship integrity.
 * Verifies unique user constraints and orphan removal logic.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Cart Repository Integration Tests")
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

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
    @DisplayName("Save - Should persist cart with correct userId")
    void shouldSaveCart() {
        Cart saved = cartRepository.save(createCart(1L));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Constraint - Should prevent duplicate carts for the same userId")
    void shouldEnforceUniqueUserIdConstraint() {
        entityManager.persist(createCart(1L));
        entityManager.flush();

        Cart duplicateCart = createCart(1L);

        // Enforce flush to trigger the DataIntegrityViolationException
        assertThatThrownBy(() -> cartRepository.saveAndFlush(duplicateCart))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Query - Should find cart using custom findByUserId method")
    void shouldFindCartByUserId() {
        entityManager.persist(createCart(2L));
        entityManager.flush();

        Optional<Cart> result = cartRepository.findByUserId(2L);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Relationships - Should correctly link multiple CartItems")
    void shouldSaveCartWithCartItems() {
        // Arrange
        Cart cart = entityManager.persist(createCart(3L));
        Product p1 = entityManager.persist(createProduct("Phone", "p-1"));

        CartItem item = CartItem.builder()
                .cart(cart)
                .product(p1)
                .quantity(2)
                .build();

        entityManager.persist(item);
        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<Cart> foundCart = cartRepository.findByUserId(3L);

        // Assert
        assertThat(foundCart).isPresent();
        assertThat(foundCart.get().getItems()).hasSize(1);
        assertThat(foundCart.get().getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Cascade Delete - Should remove items when cart is deleted (Branch: Orphan Removal)")
    void shouldRemoveCartItemsWhenCartIsDeleted() {
        // 1. Arrange: Create and link objects in memory
        Cart cart = createCart(4L);
        // Persist the cart first so it has an ID
        cart = entityManager.persist(cart);

        Product p1 = entityManager.persist(createProduct("Gadget", "g-1"));

        CartItem item = CartItem.builder()
                .cart(cart)
                .product(p1)
                .quantity(1)
                .build();

        // CRITICAL FIX: Link the item to the cart's internal list
        // Without this, Hibernate doesn't realize the Item is a "child" of this specific Cart instance
        cart.getItems().add(item);

        // Now persist the item
        entityManager.persist(item);

        // Synchronize with DB and clear cache to simulate a fresh request
        entityManager.flush();
        entityManager.clear();

        // 2. Act: Find the cart and delete it
        Cart foundCart = cartRepository.findById(cart.getId()).orElseThrow();
        cartRepository.delete(foundCart);

        // Flush again to trigger the Cascading Delete SQL
        entityManager.flush();

        // 3. Assert
        assertThat(entityManager.find(Cart.class, cart.getId())).isNull();
        assertThat(entityManager.find(CartItem.class, item.getId())).isNull();
    }
}