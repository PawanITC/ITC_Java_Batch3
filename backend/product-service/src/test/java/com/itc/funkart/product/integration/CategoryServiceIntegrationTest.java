package com.itc.funkart.product.integration;

import com.itc.funkart.common.dto.auth.response.category.CategoryResponse;
import com.itc.funkart.common.dto.auth.response.product.ProductResponse;
import com.itc.funkart.product.config.JwtConfig;
import com.itc.funkart.product.dto.request.CategoryRequest;
import com.itc.funkart.product.dto.request.ProductCreateRequest;
import com.itc.funkart.product.entity.Category;
import com.itc.funkart.product.entity.Product;
import com.itc.funkart.product.kafka.producer.CheckoutProducer;
import com.itc.funkart.product.kafka.producer.ProductProducer;
import com.itc.funkart.product.repository.CategoryRepository;
import com.itc.funkart.product.repository.ProductRepository;
import com.itc.funkart.product.service.CategoryService;
import com.itc.funkart.product.service.JwtService;
import com.itc.funkart.product.service.ProductService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CategoryServiceIntegrationTest</h2>
 * <p>
 * Validates the end-to-end lifecycle of Product Categories.
 * This suite ensures that the Service layer, Repository layer, and H2 database
 * interact correctly, specifically focusing on relational integrity and
 * Record-based DTO mapping.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration: Category Service Lifecycle")
class CategoryServiceIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtConfig jwtConfig;

    @MockitoBean
    private CheckoutProducer checkoutProducer;

    @MockitoBean
    private ProductProducer productProducer;

    // --- Helper Methods ---

    private CategoryRequest createCategoryRequest(String name, String description) {
        return CategoryRequest.builder()
                .name(name + "_" + System.nanoTime())
                .description(description)
                .build();
    }

    private ProductCreateRequest createProductRequest(Long categoryId) {
        return ProductCreateRequest.builder()
                .name("T-Shirt")
                .description("Integration test product")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(categoryId)
                .brand("TestBrand")
                .imageUrls(List.of())
                .build();
    }

    // --- Test Suite ---

    @Test
    @DisplayName("Should persist category and ensure Java Record mapping is accurate")
    void shouldCreateCategoryAndPersist() {
        CategoryRequest request = createCategoryRequest("Electronics", "Gadgets");

        CategoryResponse response = categoryService.createCategory(request);

        // Force synchronization with DB and clear L1 cache
        entityManager.flush();
        entityManager.clear();

        assertThat(response.id()).isNotNull();

        Category found = categoryRepository.findById(response.id()).orElseThrow();
        assertThat(found.getName()).contains("Electronics");
        assertThat(found.getDescription()).isEqualTo("Gadgets");
    }

    @Test
    @DisplayName("Should maintain Referential Integrity between Categories and Products")
    void shouldCreateCategoryAndAssignProducts() {
        CategoryResponse category = categoryService.createCategory(
                createCategoryRequest("Clothing", "Apparel"));

        ProductCreateRequest p1Req = createProductRequest(category.id());

        ProductResponse p1 = productService.createProduct(p1Req);

        entityManager.flush();
        entityManager.clear();

        // Verify Relationship via Service Layer
        ProductResponse retrievedProduct = productService.getProduct(p1.id());
        assertThat(retrievedProduct.categoryName()).startsWith("Clothing");

        // Verify Relationship via Repository Layer (Entity level)
        Product dbProduct = productRepository.findById(p1.id()).orElseThrow();
        assertThat(dbProduct.getCategory().getId()).isEqualTo(category.id());
    }

    @Test
    @DisplayName("Should retrieve all categories and verify list integrity")
    void shouldRetrieveAllCategories() {
        categoryService.createCategory(createCategoryRequest("Home", "Furniture"));
        categoryService.createCategory(createCategoryRequest("Garden", "Plants"));

        entityManager.flush();
        entityManager.clear();

        List<CategoryResponse> categories = categoryService.getAllCategories();

        assertThat(categories.size()).isGreaterThanOrEqualTo(2);
        assertThat(categories).anyMatch(c -> c.name().startsWith("Home"));
        assertThat(categories).anyMatch(c -> c.name().startsWith("Garden"));
    }

    @Test
    @DisplayName("Should remove category and verify record absence")
    void shouldDeleteCategoryAndVerify() {
        CategoryResponse category = categoryService.createCategory(
                createCategoryRequest("Trash", "To be deleted"));
        Long id = category.id();

        entityManager.flush();

        categoryService.deleteCategory(id);

        entityManager.flush();
        entityManager.clear();

        assertThat(categoryRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("Should handle field fidelity across DB round-trip")
    void shouldRetrieveCategoryWithAllFields() {
        String name = "Spec-Category";
        String desc = "Spec-Description";
        CategoryResponse created = categoryService.createCategory(createCategoryRequest(name, desc));

        entityManager.flush();
        entityManager.clear();

        CategoryResponse retrieved = categoryService.getCategoryById(created.id());

        assertThat(retrieved.name()).startsWith(name);
        assertThat(retrieved.description()).isEqualTo(desc);
    }
}