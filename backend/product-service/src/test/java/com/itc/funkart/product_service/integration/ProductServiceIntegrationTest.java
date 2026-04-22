package com.itc.funkart.product_service.integration;

import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.request.ProductUpdateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.dto.response.ProductsResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
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
@DisplayName("Product Service Integration Tests")
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category createCategory() {
        return categoryRepository.save(Category.builder()
                .name("Electronics_" + System.nanoTime())
                .description("Electronic devices")
                .build());
    }

    private ProductCreateRequest createProductRequest(String name, Long categoryId) {
        return ProductCreateRequest.builder()
                .name(name + "_" + System.nanoTime()) // Make unique
                .description("Test description")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(categoryId)
                .brand("TestBrand")
                .build();
    }

    @Test
    @DisplayName("Should create product with category and persist to database")
    void shouldCreateProductWithCategoryAndPersist() {
        // Arrange
        Category category = createCategory();
        ProductCreateRequest request = createProductRequest("Laptop", category.getId());

        // Act
        ProductResponse response = productService.createProduct(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();

        // Verify in database
        Product found = productRepository.findById(response.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getName()).startsWith("Laptop");
        assertThat(found.getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    @DisplayName("Should create product and verify all fields are persisted")
    void shouldCreateProductAndVerifyAllFields() {
        // Arrange
        Category category = createCategory();
        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("iPhone")
                .description("Apple iPhone 15")
                .price(BigDecimal.valueOf(999.99))
                .stockQuantity(50)
                .categoryId(category.getId())
                .brand("Apple")
                .build();

        // Act
        ProductResponse response = productService.createProduct(request);

        // Assert
        assertThat(response)
                .extracting(
                    ProductResponse::getName,
                    ProductResponse::getPrice,
                    ProductResponse::getBrand
                )
                .containsExactly("iPhone", BigDecimal.valueOf(999.99), "Apple");
        assertThat(response.getCategoryName()).startsWith("Electronics");
    }

    @Test
    @DisplayName("Should update product and verify changes in database")
    void shouldUpdateProductAndVerifyInDatabase() {
        // Arrange
        Category category = createCategory();
        ProductResponse created = productService.createProduct(
            createProductRequest("Original Product", category.getId()));

        ProductUpdateRequest updateRequest = new ProductUpdateRequest();
        updateRequest.setName("Updated Product");
        updateRequest.setPrice(BigDecimal.valueOf(299.99));

        // Act
        ProductResponse updated = productService.updateProduct(created.getId(), updateRequest);

        // Assert
        assertThat(updated.getName()).isEqualTo("Updated Product");
        assertThat(updated.getPrice()).isEqualTo(BigDecimal.valueOf(299.99));

        // Verify in database
        Product found = productRepository.findById(created.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Updated Product");
        assertThat(found.getPrice()).isEqualTo(BigDecimal.valueOf(299.99));
    }

    @Test
    @DisplayName("Should delete product and verify deletion from database")
    void shouldDeleteProductAndVerifyDeletion() {
        // Arrange
        Category category = createCategory();
        ProductResponse created = productService.createProduct(
            createProductRequest("Product to Delete", category.getId()));
        Long productId = created.getId();

        // Act
        productService.deleteProduct(productId);

        // Assert
        assertThat(productRepository.findById(productId)).isEmpty();
    }

    @Test
    @DisplayName("Should fetch products by ids and return correct items")
    void shouldFetchProductsByIdsAndReturnCorrectItems() {
        // Arrange
        Category category = createCategory();
        ProductResponse prod1 = productService.createProduct(
            createProductRequest("Product 1", category.getId()));
        ProductResponse prod2 = productService.createProduct(
            createProductRequest("Product 2", category.getId()));
        ProductResponse prod3 = productService.createProduct(
            createProductRequest("Product 3", category.getId()));

        // Act
        ProductsResponse response = productService.getProductsByIds(
            List.of(prod1.getId(), prod2.getId(), prod3.getId()));

        // Assert
        assertThat(response.getFound()).hasSize(3);
        assertThat(response.getMissing()).isEmpty();
    }

    @Test
    @DisplayName("Should fetch products by ids and handle missing ids")
    void shouldFetchProductsByIdsAndHandleMissing() {
        // Arrange
        Category category = createCategory();
        ProductResponse prod1 = productService.createProduct(
            createProductRequest("Product 1", category.getId()));

        // Act
        ProductsResponse response = productService.getProductsByIds(
            List.of(prod1.getId(), 999L, 1000L));

        // Assert
        assertThat(response.getFound()).hasSize(1);
        assertThat(response.getMissing()).hasSize(2).contains(999L, 1000L);
    }

    @Test
    @DisplayName("Should get all products ordered by creation date")
    void shouldGetAllProductsOrderedByCreationDate() throws InterruptedException {
        // Arrange
        Category category = createCategory();
        ProductResponse prod1 = productService.createProduct(
            createProductRequest("First Product", category.getId()));
        
        Thread.sleep(100); // Small delay to ensure different timestamps
        
        ProductResponse prod2 = productService.createProduct(
            createProductRequest("Second Product", category.getId()));

        // Act
        List<ProductResponse> products = productService.getAllProducts();

        // Assert
        assertThat(products).hasSizeGreaterThanOrEqualTo(2);
        assertThat(products.get(0).getName()).startsWith("Second Product"); // Most recent first
    }

    @Test
    @DisplayName("Should verify product slug is unique")
    void shouldVerifyProductSlugIsUnique() {
        // Arrange
        Category category = createCategory();
        // Create product with specific name
        ProductCreateRequest request1 = ProductCreateRequest.builder()
                .name("UniqueSlugTest")
                .description("Test description")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(category.getId())
                .brand("TestBrand")
                .build();
        
        ProductCreateRequest request2 = ProductCreateRequest.builder()
                .name("UniqueSlugTest")  // Same name should generate same slug
                .description("Test description")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(category.getId())
                .brand("TestBrand")
                .build();

        // Act
        productService.createProduct(request1);

        // Assert - Second product with same slug should fail
        assertThatThrownBy(() -> productService.createProduct(request2))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should handle product with multiple updates")
    void shouldHandleProductWithMultipleUpdates() {
        // Arrange
        Category category = createCategory();
        ProductResponse product = productService.createProduct(
            createProductRequest("Multi-Update Product", category.getId()));

        // Act - Update 1
        ProductUpdateRequest update1 = new ProductUpdateRequest();
        update1.setPrice(BigDecimal.valueOf(199.99));
        productService.updateProduct(product.getId(), update1);

        // Act - Update 2
        ProductUpdateRequest update2 = new ProductUpdateRequest();
        update2.setPrice(BigDecimal.valueOf(299.99));
        productService.updateProduct(product.getId(), update2);

        // Act - Update 3
        ProductUpdateRequest update3 = new ProductUpdateRequest();
        update3.setPrice(BigDecimal.valueOf(399.99));
        ProductResponse final_response = productService.updateProduct(product.getId(), update3);

        // Assert
        assertThat(final_response.getPrice()).isEqualTo(BigDecimal.valueOf(399.99));
    }

    @Test
    @Transactional
    @DisplayName("Should verify product category relationship")
    void shouldVerifyProductCategoryRelationship() {
        // Arrange
        Category category = createCategory();
        ProductResponse product = productService.createProduct(
            createProductRequest("Related Product", category.getId()));

        // Act
        Product found = productRepository.findById(product.getId()).orElseThrow();

        // Assert
        assertThat(found.getCategory()).isNotNull();
        assertThat(found.getCategory().getName()).startsWith("Electronics");
        assertThat(found.getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    @DisplayName("Should handle product search by slug")
    void shouldHandleProductSearchBySlug() {
        // Arrange
        Category category = createCategory();
        ProductResponse product = productService.createProduct(
            createProductRequest("Search Test Product", category.getId()));

        // Act - Slug is derived from product name (converted to lowercase with hyphens)
        var found = productRepository.findBySlug(product.getSlug());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).startsWith("Search Test Product");
    }

    @Test
    @DisplayName("Should batch update multiple products")
    void shouldBatchUpdateMultipleProducts() {
        // Arrange
        Category category = createCategory();
        ProductResponse prod1 = productService.createProduct(
            createProductRequest("Batch Product 1", category.getId()));
        ProductResponse prod2 = productService.createProduct(
            createProductRequest("Batch Product 2", category.getId()));

        // Act
        ProductUpdateRequest update1 = new ProductUpdateRequest();
        update1.setPrice(BigDecimal.valueOf(150.00));
        ProductUpdateRequest update2 = new ProductUpdateRequest();
        update2.setPrice(BigDecimal.valueOf(250.00));

        productService.updateProduct(prod1.getId(), update1);
        productService.updateProduct(prod2.getId(), update2);

        // Assert
        ProductResponse updated1 = productService.getProduct(prod1.getId());
        ProductResponse updated2 = productService.getProduct(prod2.getId());

        assertThat(updated1.getPrice()).isEqualTo(BigDecimal.valueOf(150.00));
        assertThat(updated2.getPrice()).isEqualTo(BigDecimal.valueOf(250.00));
    }
}
