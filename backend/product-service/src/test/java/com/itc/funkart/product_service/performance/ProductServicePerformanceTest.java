package com.itc.funkart.product_service.performance;

import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
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
@DisplayName("Performance Tests")
class ProductServicePerformanceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private static final long MAX_RESPONSE_TIME_MS = 500; // 500ms threshold
    private static final long MAX_BATCH_RESPONSE_TIME_MS = 1000; // 1 second for batch

    @Test
    @DisplayName("Should create product within acceptable response time")
    void shouldCreateProductWithinAcceptableTime() {
        // Arrange
        Category category = categoryRepository.save(Category.builder()
                .name("Performance Test Category")
                .description("Test category")
                .build());

        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("Performance Test Product")
                .description("Test product")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(category.getId())
                .brand("TestBrand")
                .build();

        // Act
        long startTime = System.currentTimeMillis();
        ProductResponse response = productService.createProduct(request);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(response).isNotNull();
        assertThat(duration).isLessThan(MAX_RESPONSE_TIME_MS)
                .as("Product creation should complete within %dms, but took %dms", 
                    MAX_RESPONSE_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should retrieve product within acceptable response time")
    void shouldRetrieveProductWithinAcceptableTime() {
        // Arrange
        Category category = categoryRepository.save(Category.builder()
                .name("Perf Category")
                .description("Test")
                .build());

        ProductResponse created = productService.createProduct(ProductCreateRequest.builder()
                .name("Product")
                .description("Test")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(category.getId())
                .brand("Brand")
                .build());

        // Act
        long startTime = System.currentTimeMillis();
        ProductResponse retrieved = productService.getProduct(created.getId());
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(duration).isLessThan(MAX_RESPONSE_TIME_MS)
                .as("Product retrieval should complete within %dms, but took %dms",
                    MAX_RESPONSE_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should get all products within acceptable response time")
    void shouldGetAllProductsWithinAcceptableTime() {
        // Arrange - Create multiple products
        Category category = categoryRepository.save(Category.builder()
                .name("All Products Test")
                .description("Test")
                .build());

        for (int i = 0; i < 10; i++) {
            productService.createProduct(ProductCreateRequest.builder()
                    .name("Product " + i)
                    .description("Test")
                    .price(BigDecimal.valueOf(99.99))
                    .stockQuantity(10)
                    .categoryId(category.getId())
                    .brand("Brand")
                    .build());
        }

        // Act
        long startTime = System.currentTimeMillis();
        List<ProductResponse> products = productService.getAllProducts();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(products).isNotEmpty();
        assertThat(duration).isLessThan(MAX_RESPONSE_TIME_MS)
                .as("Get all products should complete within %dms, but took %dms",
                    MAX_RESPONSE_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should batch fetch products within acceptable response time")
    void shouldBatchFetchProductsWithinAcceptableTime() {
        // Arrange
        Category category = categoryRepository.save(Category.builder()
                .name("Batch Test")
                .description("Test")
                .build());

        List<Long> ids = new java.util.ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            ids.add((long) i);
        }

        // Act
        long startTime = System.currentTimeMillis();
        var response = productService.getProductsByIds(ids);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(duration).isLessThan(MAX_BATCH_RESPONSE_TIME_MS)
                .as("Batch fetch should complete within %dms, but took %dms",
                    MAX_BATCH_RESPONSE_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should update product within acceptable response time")
    void shouldUpdateProductWithinAcceptableTime() {
        // Arrange
        Category category = categoryRepository.save(Category.builder()
                .name("Update Test")
                .description("Test")
                .build());

        ProductResponse product = productService.createProduct(ProductCreateRequest.builder()
                .name("Update Product")
                .description("Test")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(category.getId())
                .brand("Brand")
                .build());

        com.itc.funkart.product_service.dto.request.ProductUpdateRequest updateRequest = 
                new com.itc.funkart.product_service.dto.request.ProductUpdateRequest();
        updateRequest.setName("Updated Name");

        // Act
        long startTime = System.currentTimeMillis();
        ProductResponse updated = productService.updateProduct(product.getId(), updateRequest);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(updated).isNotNull();
        assertThat(duration).isLessThan(MAX_RESPONSE_TIME_MS)
                .as("Product update should complete within %dms, but took %dms",
                    MAX_RESPONSE_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should create category within acceptable response time")
    void shouldCreateCategoryWithinAcceptableTime() {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setName("Performance Category");
        request.setDescription("Test category");

        // Act
        long startTime = System.currentTimeMillis();
        var response = categoryService.createCategory(request);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(response).isNotNull();
        assertThat(duration).isLessThan(MAX_RESPONSE_TIME_MS)
                .as("Category creation should complete within %dms, but took %dms",
                    MAX_RESPONSE_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should handle bulk product creation efficiently")
    void shouldHandleBulkProductCreationEfficiently() {
        // Arrange
        Category category = categoryRepository.save(Category.builder()
                .name("Bulk Test")
                .description("Test")
                .build());

        long startTime = System.currentTimeMillis();

        // Act
        for (int i = 0; i < 50; i++) {
            productService.createProduct(ProductCreateRequest.builder()
                    .name("Bulk Product " + i)
                    .description("Test")
                    .price(BigDecimal.valueOf(99.99 + i))
                    .stockQuantity(10)
                    .categoryId(category.getId())
                    .brand("Brand")
                    .build());
        }

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        long averagePerProduct = totalDuration / 50;

        // Assert
        assertThat(averagePerProduct).isLessThan(MAX_RESPONSE_TIME_MS)
                .as("Average creation time per product should be under %dms, but was %dms",
                    MAX_RESPONSE_TIME_MS, averagePerProduct);
    }

    @Test
    @DisplayName("Should retrieve all categories within acceptable response time")
    void shouldRetrieveAllCategoriesWithinAcceptableTime() {
        // Arrange - Create multiple categories
        for (int i = 0; i < 20; i++) {
            CategoryRequest request = new CategoryRequest();
            request.setName("Category " + i);
            request.setDescription("Test");
            categoryService.createCategory(request);
        }

        // Act
        long startTime = System.currentTimeMillis();
        List<com.itc.funkart.product_service.dto.request.CategoryResponse> categories = 
                categoryService.getAllCategories();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(categories).isNotEmpty();
        assertThat(duration).isLessThan(MAX_RESPONSE_TIME_MS)
                .as("Get all categories should complete within %dms, but took %dms",
                    MAX_RESPONSE_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should measure throughput of product creation")
    void shouldMeasureThroughputOfProductCreation() {
        // Arrange
        Category category = categoryRepository.save(Category.builder()
                .name("Throughput Test")
                .description("Test")
                .build());

        int productCount = 100;
        long startTime = System.currentTimeMillis();

        // Act
        for (int i = 0; i < productCount; i++) {
            productService.createProduct(ProductCreateRequest.builder()
                    .name("Throughput Product " + i)
                    .description("Test")
                    .price(BigDecimal.valueOf(99.99))
                    .stockQuantity(10)
                    .categoryId(category.getId())
                    .brand("Brand")
                    .build());
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double throughput = (productCount * 1000.0) / totalTime; // Products per second

        // Assert
        assertThat(throughput).isGreaterThan(5.0)
                .as("Throughput should be greater than 5 products per second, but was %.2f",
                    throughput);
    }

    @Test
    @DisplayName("Should verify memory efficiency with multiple operations")
    void shouldVerifyMemoryEfficiencyWithMultipleOperations() {
        // Arrange
        Category category = categoryRepository.save(Category.builder()
                .name("Memory Test")
                .description("Test")
                .build());

        Runtime runtime = Runtime.getRuntime();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        // Act
        for (int i = 0; i < 50; i++) {
            productService.createProduct(ProductCreateRequest.builder()
                    .name("Memory Product " + i)
                    .description("Test")
                    .price(BigDecimal.valueOf(99.99))
                    .stockQuantity(10)
                    .categoryId(category.getId())
                    .brand("Brand")
                    .build());
        }

        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;

        // Assert - Should not use excessive memory (less than 100MB for 50 products)
        assertThat(memoryUsed).isLessThan(100 * 1024 * 1024)
                .as("Memory usage should be reasonable");
    }

    @Test
    @DisplayName("Should handle rapid sequential requests")
    void shouldHandleRapidSequentialRequests() {
        // Arrange
        Category category = categoryRepository.save(Category.builder()
                .name("Rapid Request Test")
                .description("Test")
                .build());

        // Act
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 100; i++) {
            productService.getProduct(1L); // Intentionally using same ID
        }

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;

        // Assert
        assertThat(totalDuration).isLessThan(5000) // 5 seconds for 100 requests
                .as("100 rapid requests should complete in 5 seconds, but took %dms",
                    totalDuration);
    }
}

