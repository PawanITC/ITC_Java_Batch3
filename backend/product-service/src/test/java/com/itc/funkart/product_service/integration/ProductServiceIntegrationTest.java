package com.itc.funkart.product_service.integration;

import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.request.ProductUpdateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.dto.response.ProductsResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.producer.OrderProducer;
import com.itc.funkart.product_service.producer.ProductProducer;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>ProductServiceIntegrationTest</h2>
 * <p>
 * Validates the core Product catalog management lifecycle.
 * This suite ensures that CRUD operations, slug generation, and batch fetching
 * interact correctly with the JPA layer and the underlying H2 database.
 * </p>
 */
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

    // --- Neutralize Infrastructure to avoid startup hangs ---
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private OrderProducer orderProducer;
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
                .imageUrls(List.of()) // Mandatory for Record mapping
                .build();
    }

    /**
     * <b>Scenario:</b> Creation and Persistence.
     * Validates that the Service correctly maps the Request DTO to the Entity
     * and establishes the Foreign Key relationship with the Category.
     */
    @Test
    @DisplayName("Create - Should persist product and link category correctly")
    void shouldCreateProductWithCategoryAndPersist() {
        Category category = createCategory();
        ProductCreateRequest request = createProductRequest("Laptop", category.getId());

        ProductResponse response = productService.createProduct(request);

        assertThat(response.id()).isNotNull();
        Product found = productRepository.findById(response.id()).orElseThrow();
        assertThat(found.getName()).contains("Laptop");
        assertThat(found.getCategory().getId()).isEqualTo(category.getId());
    }

    /**
     * <b>Scenario:</b> Batch Identification.
     * Ensures the service can return a mix of found products and a list of
     * IDs that do not exist in the database.
     */
    @Test
    @DisplayName("Read - Should fetch products by multiple IDs and handle missing ones")
    void shouldFetchProductsByIdsAndHandleMissing() {
        Category category = createCategory();
        ProductResponse prod = productService.createProduct(createProductRequest("P1", category.getId()));

        ProductsResponse response = productService.getProductsByIds(List.of(prod.id(), 999L));

        assertThat(response.found()).hasSize(1);
        assertThat(response.missing()).contains(999L);
    }

    /**
     * <b>Scenario:</b> Full Update.
     * Validates that providing a complete {@link ProductUpdateRequest} modifies
     * the persistent state across all core fields.
     */
    @Test
    @DisplayName("Update - Should update product details using the correct UpdateRequest Record")
    void shouldUpdateProductAndVerifyInDatabase() {
        Category category = createCategory();
        ProductResponse created = productService.createProduct(createProductRequest("Original", category.getId()));

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .name("Updated Product")
                .price(BigDecimal.valueOf(299.99))
                .brand("NewBrand")
                .active(true)
                .build();

        ProductResponse updated = productService.updateProduct(created.id(), updateRequest);

        assertThat(updated.name()).isEqualTo("Updated Product");
        assertThat(updated.price()).isEqualByComparingTo(BigDecimal.valueOf(299.99));

        Product foundInDb = productRepository.findById(created.id()).orElseThrow();
        assertThat(foundInDb.getName()).isEqualTo("Updated Product");
    }

    /**
     * <b>Scenario:</b> Partial (PATCH) Logic.
     * Verifies that fields not present in the update request (nulls) do not
     * overwrite existing data in the database.
     */
    @Test
    @DisplayName("Partial Update - Should only modify specified fields (Branch: PATCH logic)")
    void shouldPerformPartialUpdate() {
        Category category = createCategory();
        ProductCreateRequest createRequest = createProductRequest("Static Name", category.getId());
        ProductResponse original = productService.createProduct(createRequest);

        ProductUpdateRequest patchRequest = ProductUpdateRequest.builder()
                .price(BigDecimal.valueOf(150.00))
                .active(false)
                .build();

        ProductResponse updated = productService.updateProduct(original.id(), patchRequest);

        // Name and Brand should remain from the original creation
        assertThat(updated.name()).contains("Static Name");
        assertThat(updated.brand()).isEqualTo("TestBrand");
        assertThat(updated.price()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        assertThat(updated.active()).isFalse();
    }

    /**
     * <b>Scenario:</b> Deletion.
     * Confirms that the product is physically removed from the repository.
     */
    @Test
    @DisplayName("Delete - Should remove product from catalog")
    void shouldDeleteProductAndVerifyDeletion() {
        Category category = createCategory();
        ProductResponse created = productService.createProduct(createProductRequest("To Delete", category.getId()));

        productService.deleteProduct(created.id());

        assertThat(productRepository.findById(created.id())).isEmpty();
    }

    /**
     * <b>Scenario:</b> Sorting.
     * Ensures that the default retrieval order is Newest-to-Oldest based on
     * the creation timestamp.
     */
    @Test
    @DisplayName("Query - Should return all products with descending date order")
    void shouldGetAllProductsOrderedByCreationDate() throws InterruptedException {
        Category category = createCategory();
        productService.createProduct(createProductRequest("Oldest", category.getId()));
        Thread.sleep(10); // Guarantee a distinct timestamp
        productService.createProduct(createProductRequest("Newest", category.getId()));

        List<ProductResponse> products = productService.getAllProducts();

        assertThat(products.get(0).name()).contains("Newest");
    }

    /**
     * <b>Scenario:</b> Data Integrity.
     * Validates database-level constraints, specifically that unique slugs
     * (derived from names) prevent duplicate catalog entries.
     */
    @Test
    @DisplayName("Constraint - Should prevent duplicate slugs via identical names")
    void shouldVerifyProductSlugIsUnique() {
        Category category = createCategory();
        String name = "SlugMaster";

        productService.createProduct(ProductCreateRequest.builder()
                .name(name).price(BigDecimal.ONE).categoryId(category.getId())
                .brand("B").imageUrls(List.of()).build());

        assertThatThrownBy(() -> productService.createProduct(ProductCreateRequest.builder()
                .name(name).price(BigDecimal.ONE).categoryId(category.getId())
                .brand("B").imageUrls(List.of()).build()))
                .isInstanceOf(Exception.class);
    }
}