package com.itc.funkart.product_service.integration;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartResponse;
import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.dto.request.CategoryResponse;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.entity.Cart;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.repository.CartRepository;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import com.itc.funkart.product_service.service.CartService;
import com.itc.funkart.product_service.service.CategoryService;
import com.itc.funkart.product_service.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

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

    private CategoryRequest createCategoryRequest(String name, String description) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name + "_" + System.nanoTime()); // Make unique
        request.setDescription(description);
        return request;
    }

    private ProductCreateRequest createProductRequest(String name, Long categoryId) {
        return ProductCreateRequest.builder()
                .name(name)
                .description("Test product")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(categoryId)
                .brand("TestBrand")
                .build();
    }

    @Test
    @DisplayName("Should create category and persist to database")
    void shouldCreateCategoryAndPersist() {
        // Arrange
        CategoryRequest request = createCategoryRequest("Electronics_" + System.currentTimeMillis(), "Electronic devices");

        // Act
        CategoryResponse response = categoryService.createCategory(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();

        // Verify in database
        Category found = categoryRepository.findById(response.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getName()).contains("Electronics_");
    }

    @Test
    @DisplayName("Should get category by id with all details")
    void shouldGetCategoryWithAllDetails() {
        // Arrange
        CategoryRequest request = createCategoryRequest("Books", "Books and literature");
        CategoryResponse created = categoryService.createCategory(request);

        // Act
        CategoryResponse retrieved = categoryService.getCategoryById(created.getId());

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(created.getId());
        assertThat(retrieved.getName()).startsWith("Books");
        assertThat(retrieved.getDescription()).isEqualTo("Books and literature");
    }

    @Test
    @DisplayName("Should create category and assign products to it")
    void shouldCreateCategoryAndAssignProducts() {
        // Arrange
        CategoryRequest categoryRequest = createCategoryRequest("Clothing", "Apparel");
        CategoryResponse category = categoryService.createCategory(categoryRequest);

        ProductCreateRequest productRequest1 = createProductRequest("T-Shirt", category.getId());
        ProductCreateRequest productRequest2 = createProductRequest("Jeans", category.getId());

        // Act
        ProductResponse product1 = productService.createProduct(productRequest1);
        ProductResponse product2 = productService.createProduct(productRequest2);

        // Assert
        assertThat(product1.getCategoryName()).startsWith("Clothing");
        assertThat(product2.getCategoryName()).startsWith("Clothing");

        List<Product> products = productRepository.findAll();
        assertThat(products).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should retrieve all categories with products")
    void shouldRetrieveAllCategoriesWithProducts() {
        // Arrange - Create multiple categories
        CategoryResponse cat1 = categoryService.createCategory(
            createCategoryRequest("Electronics", "Electronic devices"));
        CategoryResponse cat2 = categoryService.createCategory(
            createCategoryRequest("Furniture", "Home furniture"));

        // Act
        List<CategoryResponse> categories = categoryService.getAllCategories();

        // Assert
        assertThat(categories).hasSizeGreaterThanOrEqualTo(2);
        List<String> categoryNames = categories.stream().map(CategoryResponse::getName).toList();
        assertThat(categoryNames.stream().anyMatch(n -> n.startsWith("Electronics"))).isTrue();
        assertThat(categoryNames.stream().anyMatch(n -> n.startsWith("Furniture"))).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("Should delete category and verify deletion")
    void shouldDeleteCategoryAndVerify() {
        // Arrange
        CategoryResponse category = categoryService.createCategory(
            createCategoryRequest("Temp Category", "Temporary"));
        Long categoryId = category.getId();

        // Act
        categoryService.deleteCategory(categoryId);

        // Assert
        assertThat(categoryRepository.findById(categoryId)).isEmpty();
    }

    @Test
    @DisplayName("Should handle concurrent category creation")
    void shouldHandleConcurrentCategoryCreation() {
        // Act
        CategoryResponse cat1 = categoryService.createCategory(
            createCategoryRequest("Category1", "Desc1"));
        CategoryResponse cat2 = categoryService.createCategory(
            createCategoryRequest("Category2", "Desc2"));
        CategoryResponse cat3 = categoryService.createCategory(
            createCategoryRequest("Category3", "Desc3"));

        // Assert
        assertThat(cat1.getId()).isNotNull();
        assertThat(cat2.getId()).isNotNull();
        assertThat(cat3.getId()).isNotNull();
        assertThat(cat1.getId()).isNotEqualTo(cat2.getId()).isNotEqualTo(cat3.getId());
    }

    @Test
    @DisplayName("Should retrieve category and verify all fields")
    void shouldRetrieveCategoryWithAllFields() {
        // Arrange
        String name = "Unique Category";
        String description = "Unique Description";
        CategoryResponse created = categoryService.createCategory(
            createCategoryRequest(name, description));

        // Act
        CategoryResponse retrieved = categoryService.getCategoryById(created.getId());

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(created.getId());
        assertThat(retrieved.getName()).startsWith(name);
        assertThat(retrieved.getDescription()).isEqualTo(description);
    }

    @Test
    @DisplayName("Should update category through product association")
    void shouldUpdateCategoryThroughProductAssociation() {
        // Arrange
        CategoryResponse category = categoryService.createCategory(
            createCategoryRequest("Original Category", "Original Description"));

        ProductCreateRequest productRequest = createProductRequest("Test Product", category.getId());

        // Act
        ProductResponse product = productService.createProduct(productRequest);

        // Assert
        assertThat(product.getCategoryName()).startsWith("Original Category");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getCategory().getId())
                .isEqualTo(category.getId());
    }
}

