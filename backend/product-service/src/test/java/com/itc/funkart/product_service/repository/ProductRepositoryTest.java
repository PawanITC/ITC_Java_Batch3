package com.itc.funkart.product_service.repository;

import com.itc.funkart.product_service.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

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
    @DisplayName("Should save product and set createdAt automatically")
    void shouldSaveProductAndSetCreatedAt() {
        Product product = createProduct("Phone", "phone-slug");

        Product saved = productRepository.save(product);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find product by slug")
    void shouldFindProductBySlug() {
        Product product = createProduct("Laptop", "laptop-slug");
        productRepository.save(product);

        Optional<Product> result = productRepository.findBySlug("laptop-slug");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("Should return empty when slug not found")
    void shouldReturnEmptyWhenSlugNotFound() {
        Optional<Product> result = productRepository.findBySlug("not-found");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should enforce unique slug constraint")
    void shouldThrowExceptionWhenSlugDuplicate() {
        Product p1 = createProduct("Item1", "same-slug");
        Product p2 = createProduct("Item2", "same-slug");

        productRepository.save(p1);

        assertThatThrownBy(() -> productRepository.saveAndFlush(p2))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should return products ordered by createdAt desc")
    void shouldReturnProductsInDescendingOrder() {
        Product oldProduct = createProduct("Old", "old-slug");
        Product newProduct = createProduct("New", "new-slug");

        productRepository.save(oldProduct);
        productRepository.save(newProduct);

        List<Product> result = productRepository.findAllByOrderByCreatedAtDesc();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCreatedAt())
                .isAfterOrEqualTo(result.get(1).getCreatedAt());
    }
}
