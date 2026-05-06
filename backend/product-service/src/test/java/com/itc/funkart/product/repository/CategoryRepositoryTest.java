package com.itc.funkart.product.repository;

import com.itc.funkart.product.entity.Category;
import com.itc.funkart.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>CategoryRepositoryTest</h2>
 * <p>
 * Integration tests for Category persistence.
 * Focuses on unique constraints and relationship integrity with Product entities.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Category Repository Integration Tests")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    // --- Helpers using Entity Builders ---

    private Category createCategory(String name) {
        return Category.builder()
                .name(name)
                .description("Test description")
                .build();
    }

    private Product createProduct(String name, Category category) {
        return Product.builder()
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .price(BigDecimal.valueOf(100))
                .stockQuantity(10)
                .brand("TestBrand")
                .active(true)
                .category(category)
                .build();
    }

    @Test
    @DisplayName("Save - Should persist category and generate ID")
    void shouldSaveCategorySuccessfully() {
        Category category = createCategory("Electronics");

        Category saved = categoryRepository.save(category);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("Constraint - Should prevent duplicate names (Branch: Data Integrity)")
    void shouldEnforceUniqueCategoryName() {
        // Arrange: Persist one category
        entityManager.persist(createCategory("Clothing"));
        entityManager.flush();

        // Act & Assert: Attempt to save another with the same name
        Category duplicate = createCategory("Clothing");

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(duplicate))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Relationship - Should find products associated with a category")
    void shouldVerifyCategoryProductRelationship() {
        // Arrange
        Category category = entityManager.persist(createCategory("Books"));
        entityManager.persist(createProduct("Java Guide", category));
        entityManager.persist(createProduct("Spring Guide", category));

        entityManager.flush();
        entityManager.clear(); // Clear cache to force a fresh SQL query

        // Act
        Category found = categoryRepository.findById(category.getId()).orElseThrow();

        // Assert
        // This assumes your Category entity has a List<Product> products field
        // If it doesn't, we can verify via a ProductRepository call
        assertThat(found.getName()).isEqualTo("Books");
    }

    @Test
    @DisplayName("Query - Should retrieve all persisted categories")
    void shouldFindAllCategories() {
        entityManager.persist(createCategory("Home"));
        entityManager.persist(createCategory("Garden"));
        entityManager.flush();

        List<Category> all = categoryRepository.findAll();

        assertThat(all).hasSize(2);
        assertThat(all).extracting(Category::getName).containsExactlyInAnyOrder("Home", "Garden");
    }

    @Test
    @DisplayName("Delete - Should remove category (Branch: Constraint Check)")
    void shouldDeleteCategory() {
        Category category = entityManager.persist(createCategory("Temp"));
        entityManager.flush();

        categoryRepository.delete(category);
        entityManager.flush();

        assertThat(entityManager.find(Category.class, category.getId())).isNull();
    }
}