package com.itc.funkart.product_service.integration;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.response.CartResponse;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.dto.response.ProductsResponse;
import com.itc.funkart.product_service.producer.OrderProducer;
import com.itc.funkart.product_service.producer.ProductProducer;
import com.itc.funkart.product_service.repository.CartRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import com.itc.funkart.product_service.service.CartService;
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
 * <h2>Cross-Feature Integration Tests</h2>
 * <p>
 * This suite validates the deep integration between Product, Category, and Cart features.
 * Unlike unit tests, these interact with the actual database layer and transaction manager
 * to ensure that JPA mappings, cascades, and business logic hold up under real scenarios.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Cross-Feature Integration Tests")
class CrossFeatureIntegrationTest {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CartService cartService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartRepository cartRepository;

    @MockitoBean
    private OrderProducer orderProducer;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ProductProducer productProducer;

    /**
     * <b>Scenario:</b> Complete User Journey.
     * <ol>
     * <li>Admin creates a catalog (Category + Products).</li>
     * <li>User adds multiple items to their cart.</li>
     * <li>User executes checkout, clearing the cart state.</li>
     * </ol>
     */
    @Test
    @DisplayName("Workflow - Should complete full journey from Catalog creation to Cart checkout")
    void shouldCompleteFullECommerceWorkflow() {
        // 1. Create Category
        var category = categoryService.createCategory(CategoryRequest.builder()
                .name("Electronics_" + System.nanoTime())
                .description("Devices")
                .build());

        // 2. Create Products (Ensuring all record fields are populated via Builder)
        ProductResponse laptop = productService.createProduct(ProductCreateRequest.builder()
                .name("Laptop")
                .price(BigDecimal.valueOf(999.99))
                .categoryId(category.id())
                .stockQuantity(5)
                .brand("Dell")
                .imageUrls(List.of())
                .build());

        ProductResponse mouse = productService.createProduct(ProductCreateRequest.builder()
                .name("Mouse")
                .price(BigDecimal.valueOf(49.99))
                .categoryId(category.id())
                .stockQuantity(100)
                .brand("Logitech")
                .imageUrls(List.of())
                .build());

        // 3. Add to Cart (Verifying state accumulation)
        Long userId = 1000L;
        cartService.addItemToCart(userId, AddToCartRequest.builder()
                .productId(laptop.id()).quantity(1).build());

        CartResponse cart = cartService.addItemToCart(userId, AddToCartRequest.builder()
                .productId(mouse.id()).quantity(2).build());

        // 4. Verify logic
        assertThat(cart.items()).hasSize(2);

        // 5. Checkout (Verifying side effects)
        cartService.checkout(userId);
        assertThat(cartService.getCartByUserId(userId).items()).isEmpty();
    }

    /**
     * <b>Scenario:</b> Batch Processing and Error Tolerance.
     * Validates that the system correctly identifies which products exist in the DB
     * and which requested IDs are invalid/missing.
     */
    @Test
    @DisplayName("Workflow - Should handle product creation and batch fetch identifying missing IDs")
    void shouldHandleProductCreationAndBatchFetch() {
        var category = categoryService.createCategory(CategoryRequest.builder()
                .name("BatchTest")
                .build());

        ProductResponse shirt = productService.createProduct(ProductCreateRequest.builder()
                .name("T-Shirt")
                .price(BigDecimal.valueOf(25.00))
                .categoryId(category.id())
                .brand("Funkart-Clothing")
                .imageUrls(List.of())
                .build());

        List<Long> requestedIds = List.of(shirt.id(), 999L); // One real, one fake

        ProductsResponse response = productService.getProductsByIds(requestedIds);

        assertThat(response.found()).hasSize(1);
        assertThat(response.found().get(0).getName()).isEqualTo("T-Shirt");
        assertThat(response.missing()).contains(999L);
    }

    /**
     * <b>Scenario:</b> JPA Relationship Integrity.
     * Ensures that lazy/eager loading and object mappings (Product -> Category)
     * are maintained even after multiple service layers have touched the data.
     */
    @Test
    @DisplayName("Consistency - Should verify DB relationships remain intact across service calls")
    void shouldVerifyDataConsistency() {
        Long userId = 5000L;
        var cat = categoryService.createCategory(CategoryRequest.builder().name("Consistency").build());
        var prod = productService.createProduct(ProductCreateRequest.builder()
                .name("Audit-Item")
                .categoryId(cat.id())
                .price(BigDecimal.TEN)
                .brand("Generic")
                .imageUrls(List.of())
                .build());

        cartService.addItemToCart(userId, AddToCartRequest.builder().productId(prod.id()).quantity(5).build());

        // Direct Repository Verification to bypass Service caching
        var cartInDb = cartRepository.findByUserId(userId).orElseThrow();
        assertThat(cartInDb.getItems().get(0).getProduct().getCategory().getName()).isEqualTo("Consistency");
    }
}