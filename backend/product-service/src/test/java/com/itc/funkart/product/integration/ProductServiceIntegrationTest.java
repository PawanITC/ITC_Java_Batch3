package com.itc.funkart.product.integration;

import com.itc.funkart.common.dto.auth.response.product.ProductResponse;
import com.itc.funkart.common.dto.auth.response.product.ProductsResponse;
import com.itc.funkart.product.config.JwtConfig;
import com.itc.funkart.product.dto.request.ProductCreateRequest;
import com.itc.funkart.product.dto.request.ProductUpdateRequest;
import com.itc.funkart.product.entity.Category;
import com.itc.funkart.product.entity.Product;
import com.itc.funkart.product.kafka.producer.CheckoutProducer;
import com.itc.funkart.product.kafka.producer.ProductProducer;
import com.itc.funkart.product.repository.CategoryRepository;
import com.itc.funkart.product.repository.ProductRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>ProductServiceIntegrationTest</h2>
 * <p>
 * Validates the core Product catalog management lifecycle.
 * Focuses on JPA mapping fidelity, slug generation, and transactional integrity.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration: Product Service Catalog")
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
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

    // --- Helpers ---

    private Category createCategory() {
        return categoryRepository.save(Category.builder()
                .name("Electronics_" + System.nanoTime())
                .description("Devices")
                .build());
    }

    private ProductCreateRequest createProductRequest(String name, Long categoryId) {
        return ProductCreateRequest.builder()
                .name(name + "_" + System.nanoTime())
                .description("Test description")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(categoryId)
                .brand("TestBrand")
                .imageUrls(List.of())
                .build();
    }

    @Test
    @DisplayName("Create: Should persist product and verify DB mapping")
    void shouldCreateProductWithCategoryAndPersist() {
        Category category = createCategory();
        ProductCreateRequest request = createProductRequest("Laptop", category.getId());

        ProductResponse response = productService.createProduct(request);

        entityManager.flush();
        entityManager.clear();

        Product found = productRepository.findById(response.id()).orElseThrow();
        assertThat(found.getName()).contains("Laptop");
        assertThat(found.getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    @DisplayName("Read: Should handle mixed existing and non-existing IDs")
    void shouldFetchProductsByIdsAndHandleMissing() {
        Category category = createCategory();
        ProductResponse prod = productService.createProduct(createProductRequest("P1", category.getId()));

        entityManager.flush();
        entityManager.clear();

        ProductsResponse response = productService.getProductsByIds(List.of(prod.id(), 999L));

        assertThat(response.found()).hasSize(1);
        assertThat(response.missing()).contains(999L);
    }

    @Test
    @DisplayName("Update: Should perform full update and clear L1 cache")
    void shouldUpdateProductAndVerifyInDatabase() {
        Category category = createCategory();
        ProductResponse created = productService.createProduct(createProductRequest("Original", category.getId()));

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .name("Updated Product")
                .price(BigDecimal.valueOf(299.99))
                .brand("NewBrand")
                .active(true)
                .build();

        productService.updateProduct(created.id(), updateRequest);

        entityManager.flush();
        entityManager.clear();

        Product foundInDb = productRepository.findById(created.id()).orElseThrow();
        assertThat(foundInDb.getName()).isEqualTo("Updated Product");
        assertThat(foundInDb.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(299.99));
    }

    @Test
    @DisplayName("Partial Update: Should preserve non-modified fields")
    void shouldPerformPartialUpdate() {
        Category category = createCategory();
        ProductResponse original = productService.createProduct(createProductRequest("Static Name", category.getId()));

        ProductUpdateRequest patchRequest = ProductUpdateRequest.builder()
                .price(BigDecimal.valueOf(150.00))
                .active(false)
                .build();

        productService.updateProduct(original.id(), patchRequest);

        entityManager.flush();
        entityManager.clear();

        Product found = productRepository.findById(original.id()).orElseThrow();
        assertThat(found.getName()).contains("Static Name"); // Remained the same
        assertThat(found.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        assertThat(found.getActive()).isFalse();
    }

    @Test
    @DisplayName("Delete: Should physically remove product from DB")
    void shouldDeleteProductAndVerifyDeletion() {
        Category category = createCategory();
        ProductResponse created = productService.createProduct(createProductRequest("To Delete", category.getId()));

        entityManager.flush();
        productService.deleteProduct(created.id());
        entityManager.flush();
        entityManager.clear();

        assertThat(productRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("Constraint: Should fail when duplicate slugs are generated")
    void shouldVerifyProductSlugIsUnique() {
        Category category = createCategory();
        String name = "SlugMaster";

        productService.createProduct(ProductCreateRequest.builder()
                .name(name).price(BigDecimal.ONE).categoryId(category.getId())
                .brand("B").imageUrls(List.of()).build());

        entityManager.flush(); // Synchronize state to trigger unique constraint check

        assertThatThrownBy(() -> {
            productService.createProduct(ProductCreateRequest.builder()
                    .name(name).price(BigDecimal.ONE).categoryId(category.getId())
                    .brand("B").imageUrls(List.of()).build());
            entityManager.flush(); // Force error inside the lambda
        }).isInstanceOf(Exception.class);
    }
}