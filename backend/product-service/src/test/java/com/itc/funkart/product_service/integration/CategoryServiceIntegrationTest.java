package com.itc.funkart.product_service.integration;

import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.response.CategoryResponse;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.producer.OrderProducer;
import com.itc.funkart.product_service.producer.ProductProducer;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import com.itc.funkart.product_service.service.CategoryService;
import com.itc.funkart.product_service.service.JwtService;
import com.itc.funkart.product_service.service.ProductService;
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
 * This suite validates the lifecycle of product categories and their
 * fundamental relationship with products. It ensures that the persistence
 * layer correctly handles record-to-entity mappings and database constraints.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Category Service Integration Tests")
class CategoryServiceIntegrationTest {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;

    // --- Neutralize Infrastructure to allow context reuse and speed up execution ---
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private OrderProducer orderProducer;
    @MockitoBean
    private ProductProducer productProducer;

    // --- Helpers using updated Record Builders ---

    private CategoryRequest createCategoryRequest(String name, String description) {
        return CategoryRequest.builder()
                .name(name + "_" + System.nanoTime())
                .description(description)
                .build();
    }

    private ProductCreateRequest createProductRequest(String name, Long categoryId) {
        return ProductCreateRequest.builder()
                .name(name)
                .description("Integration test product")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(categoryId)
                .brand("TestBrand")
                .imageUrls(List.of()) // Vital for record constructor
                .build();
    }

    // --- Tests ---

    /**
     * <b>Scenario:</b> Basic CRUD Persistence.
     * Ensures that the Service layer correctly saves a category and that
     * H2 generates the ID as expected.
     */
    @Test
    @DisplayName("Lifecycle - Should create, persist, and retrieve a category")
    void shouldCreateCategoryAndPersist() {
        CategoryRequest request = createCategoryRequest("Electronics", "Gadgets");

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();

        Category found = categoryRepository.findById(response.id()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getName()).contains("Electronics");
    }

    /**
     * <b>Scenario:</b> One-to-Many Relationship.
     * Validates that products created via the {@link ProductService} correctly
     * reference the persisted category's primary key.
     */
    @Test
    @DisplayName("Relationships - Should link products to category successfully")
    void shouldCreateCategoryAndAssignProducts() {
        CategoryResponse category = categoryService.createCategory(
                createCategoryRequest("Clothing", "Apparel"));

        ProductCreateRequest p1Req = createProductRequest("T-Shirt", category.id());
        ProductCreateRequest p2Req = createProductRequest("Jeans", category.id());

        ProductResponse p1 = productService.createProduct(p1Req);
        ProductResponse p2 = productService.createProduct(p2Req);

        assertThat(p1.categoryName()).startsWith("Clothing");
        assertThat(p2.categoryName()).startsWith("Clothing");

        Product dbProduct = productRepository.findById(p1.id()).orElseThrow();
        assertThat(dbProduct.getCategory().getId()).isEqualTo(category.id());
    }

    /**
     * <b>Scenario:</b> Batch Retrieval.
     * Ensures the global catalog retrieval works and correctly maps JPA entities
     * back to Java Record DTOs using the generated accessors (.name()).
     */
    @Test
    @DisplayName("Query - Should retrieve all categories and verify Record content")
    void shouldRetrieveAllCategories() {
        categoryService.createCategory(createCategoryRequest("Home", "Furniture"));
        categoryService.createCategory(createCategoryRequest("Garden", "Plants"));

        List<CategoryResponse> categories = categoryService.getAllCategories();

        assertThat(categories.size()).isGreaterThanOrEqualTo(2);
        boolean hasHome = categories.stream().anyMatch(c -> c.name().startsWith("Home"));
        assertThat(hasHome).isTrue();
    }

    /**
     * <b>Scenario:</b> Resource Cleanup.
     * Confirms that deleting a category removes it from the repository
     * (Note: If you had products, you'd need to handle Cascade logic here).
     */
    @Test
    @DisplayName("Lifecycle - Should delete category and clean up DB")
    void shouldDeleteCategoryAndVerify() {
        CategoryResponse category = categoryService.createCategory(
                createCategoryRequest("Trash", "To be deleted"));
        Long id = category.id();

        categoryService.deleteCategory(id);

        assertThat(categoryRepository.findById(id)).isEmpty();
    }

    /**
     * <b>Scenario:</b> Data Fidelity.
     * Validates that all metadata (like descriptions) survive the
     * round-trip through the database.
     */
    @Test
    @DisplayName("Integrity - Should handle multiple fields correctly in Record response")
    void shouldRetrieveCategoryWithAllFields() {
        String name = "Spec-Category";
        String desc = "Spec-Description";
        CategoryResponse created = categoryService.createCategory(createCategoryRequest(name, desc));

        CategoryResponse retrieved = categoryService.getCategoryById(created.id());

        assertThat(retrieved.name()).startsWith(name);
        assertThat(retrieved.description()).isEqualTo(desc);
    }
}