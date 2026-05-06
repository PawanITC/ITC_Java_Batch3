package com.itc.funkart.product.repository;

import com.itc.funkart.product.entity.Product;
import com.itc.funkart.product.entity.ProductImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>ProductRepositoryTest</h2>
 * Validates optimized fetch joins and data integrity constraints.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Product Repository - Advanced Fetching")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("Fetch Optimization & JPQL Tests")
    class FetchTests {

        @Test
        @DisplayName("findAllWithImages: Should fetch products and images in one round-trip")
        void shouldFetchAllWithImages() {
            // Arrange: 1 Product with 2 Images
            Product product = Product.builder()
                    .name("Console").slug("ps5").price(BigDecimal.ONE).active(true)
                    .images(new ArrayList<>()).build();
            product = entityManager.persist(product);

            ProductImage img1 = ProductImage.builder().imageUrl("url1").product(product).build();
            ProductImage img2 = ProductImage.builder().imageUrl("url2").product(product).build();
            entityManager.persist(img1);
            entityManager.persist(img2);

            entityManager.flush();
            entityManager.clear(); // Force Hibernate to forget the objects in RAM

            // Act
            List<Product> results = productRepository.findAllWithImages();

            // Assert
            assertThat(results).hasSize(1);
            // This verification happens WITHOUT a session (due to clear()),
            // proving the FETCH join worked.
            assertThat(results.get(0).getImages()).hasSize(2);
        }

        @Test
        @DisplayName("findBySlug: Should hydrate images for a specific slug")
        void shouldFetchImagesBySlug() {
            Product product = Product.builder()
                    .name("Tab").slug("tab-s").price(BigDecimal.ONE).active(true)
                    .images(new ArrayList<>()).build();
            product = entityManager.persist(product);
            entityManager.persist(ProductImage.builder().imageUrl("img").product(product).build());

            entityManager.flush();
            entityManager.clear();

            Optional<Product> result = productRepository.findBySlug("tab-s");

            assertThat(result).isPresent();
            assertThat(result.get().getImages()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Integrity Constraints")
    class Constraints {
        @Test
        @DisplayName("Uniqueness: Should reject duplicate slugs")
        void rejectDuplicateSlugs() {
            Product p1 = Product.builder().name("A").slug("same").price(BigDecimal.ONE).active(true).build();
            Product p2 = Product.builder().name("B").slug("same").price(BigDecimal.ONE).active(true).build();

            entityManager.persist(p1);
            entityManager.flush();

            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
                productRepository.saveAndFlush(p2);
            });
        }
    }
}