package com.itc.funkart.product_service.repository;

import com.itc.funkart.product_service.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>ProductRepositoryTest</h2>
 * <p>
 * Validates product persistence, slug lookups, and audit timestamp sorting.
 * Ensures the core catalog data integrity rules are enforced at the DB level.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Product Repository Integration Tests")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    // --- Helpers ---

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
    @DisplayName("Save - Should auto-generate createdAt via JPA Auditing")
    void shouldSaveProductAndSetCreatedAt() {
        Product saved = productRepository.save(createProduct("Phone", "phone-slug"));
        entityManager.flush();

        assertThat(saved.getId()).isNotNull();
        // Verifying the audit branch
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Lookup - Should retrieve product by slug (Branch: Found)")
    void shouldFindProductBySlug() {
        entityManager.persist(createProduct("Laptop", "laptop-slug"));
        entityManager.flush();

        Optional<Product> result = productRepository.findBySlug("laptop-slug");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("Lookup - Should return empty for missing slug (Branch: Not Found)")
    void shouldReturnEmptyWhenSlugNotFound() {
        Optional<Product> result = productRepository.findBySlug("ghost-item");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Constraint - Should prevent duplicate slugs (Branch: Slug Uniqueness)")
    void shouldEnforceUniqueSlugConstraint() {
        entityManager.persist(createProduct("Original", "same-slug"));
        entityManager.flush();

        Product duplicate = createProduct("Copy", "same-slug");

        assertThatThrownBy(() -> productRepository.saveAndFlush(duplicate))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Query - Should return products ordered by date (Branch: Descending Order)")
    void shouldReturnProductsInDescendingOrder() {
        // Arrange
        Product p1 = createProduct("Oldest", "old-slug");
        Product p2 = createProduct("Newest", "new-slug");

        productRepository.save(p1);
        // Ensure some time gap or sequential ID order if timestamps are identical
        productRepository.save(p2);

        entityManager.flush();
        entityManager.clear();

        // Act
        List<Product> result = productRepository.findAllByOrderByCreatedAtDesc();

        // Assert
        assertThat(result).hasSize(2);
        // The first element in the list should be the "Newest" one
        assertThat(result.get(0).getSlug()).isEqualTo("new-slug");
    }

    @Test
    @DisplayName("Batch - Should find all products by a list of IDs")
    void shouldFindAllByIds() {
        Product p1 = entityManager.persist(createProduct("A", "a-slug"));
        Product p2 = entityManager.persist(createProduct("B", "b-slug"));
        entityManager.flush();

        List<Product> found = productRepository.findAllById(List.of(p1.getId(), p2.getId()));

        assertThat(found).hasSize(2);
    }
}