package com.itc.funkart.product_service.repository;

import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Category createCategory(String name) {
        return Category.builder()
                .name(name)
                .description("Test category")
                .build();
    }

    private Product createProduct(Category category) {
        return Product.builder()
                .name("Phone")
                .slug("phone-" + System.nanoTime())
                .price(BigDecimal.valueOf(100))
                .stockQuantity(10)
                .brand("TestBrand")
                .category(category)
                .build();
    }

    @Test
    @DisplayName("Should save category")
    void shouldSaveCategory() {
        Category category = createCategory("Electronics");

        Category saved = categoryRepository.save(category);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("Should enforce unique category name")
    void shouldThrowExceptionWhenDuplicateName() {
        Category c1 = createCategory("Clothing");
        Category c2 = createCategory("Clothing");

        categoryRepository.save(c1);

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(c2))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should persist category with products")
    void shouldSaveCategoryWithProducts() {
        Category category = categoryRepository.save(createCategory("Books"));

        Product product = createProduct(category);
        productRepository.save(product);

        Category found = categoryRepository.findById(category.getId()).orElseThrow();

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(category.getId());
    }

    @Test
    @DisplayName("Should find all categories")
    void shouldFindAllCategories() {
        categoryRepository.save(createCategory("Cat1"));
        categoryRepository.save(createCategory("Cat2"));

        assertThat(categoryRepository.findAll()).hasSize(2);
    }
}
