package com.itc.funkart.product.repository;

import com.itc.funkart.product.entity.Cart;
import com.itc.funkart.product.entity.CartItem;
import com.itc.funkart.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CartRepositoryTest</h2>
 * <p>
 * Validates data access logic for {@link Cart} entities.
 * Focuses on JPQL fetch join integrity and relationship mapping.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Cart Repository - Persistence & Fetch Analysis")
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("Fetch Optimization Logic")
    class FetchLogic {

        @Test
        @DisplayName("JOIN FETCH: Should retrieve Cart, Items, and Products in a single query")
        void shouldHydrateFullGraph() {
            // 1. Arrange: Setup complex relationship
            Product product = Product.builder()
                    .name("Laptop").slug("laptop").price(BigDecimal.TEN).active(true).build();
            product = entityManager.persist(product);

            Cart cart = Cart.builder().userId(100L).items(new ArrayList<>()).build();
            cart = entityManager.persist(cart);

            CartItem item = CartItem.builder().cart(cart).product(product).quantity(1).build();
            cart.getItems().add(item);
            entityManager.persist(item);

            entityManager.flush();
            entityManager.clear(); // CRITICAL: Detach all entities to force a DB hit

            // 2. Act
            Optional<Cart> result = cartRepository.findByUserIdWithItems(100L);

            // 3. Assert
            assertThat(result).isPresent();
            assertThat(result.get().getItems()).hasSize(1);

            // This would throw LazyInitializationException if JOIN FETCH failed
            assertThat(result.get().getItems().get(0).getProduct().getName()).isEqualTo("Laptop");
        }

        @Test
        @DisplayName("LEFT JOIN: Should still return Cart even if it has no items")
        void shouldReturnEmptyCart() {
            Cart emptyCart = Cart.builder().userId(200L).items(new ArrayList<>()).build();
            entityManager.persist(emptyCart);
            entityManager.flush();
            entityManager.clear();

            Optional<Cart> result = cartRepository.findByUserIdWithItems(200L);

            assertThat(result).isPresent();
            assertThat(result.get().getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Lifecycle & Constraints")
    class Constraints {

        @Test
        @DisplayName("Orphan Removal: Should delete items from DB when removed from list")
        void shouldRemoveOrphans() {
            // Arrange
            Cart cart = entityManager.persist(Cart.builder().userId(300L).items(new ArrayList<>()).build());
            Product p = entityManager.persist(Product.builder().name("X").slug("x").price(BigDecimal.ONE).active(true).build());
            CartItem item = entityManager.persist(CartItem.builder().cart(cart).product(p).quantity(5).build());
            cart.getItems().add(item);

            entityManager.flush();

            // Act: Remove item from the list and save
            cart.getItems().clear();
            cartRepository.saveAndFlush(cart);
            entityManager.clear();

            // Assert
            assertThat(entityManager.find(CartItem.class, item.getId())).isNull();
        }
    }
}