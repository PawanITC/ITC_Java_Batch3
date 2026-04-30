package com.itc.funkart.product_service.performance;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.producer.OrderProducer;
import com.itc.funkart.product_service.producer.ProductProducer;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.service.CartService;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CartServicePerformanceTest</h2>
 * <p>
 * Benchmarks the Cart management lifecycle, evaluating the latency and throughput
 * of item additions and the checkout process under significant data volume.
 * </p>
 * <p>
 * Uses MockitoBeans to neutralize external side effects (Kafka/Security) to focus
 * strictly on service-layer efficiency and JPA persistence overhead.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Performance: Cart Operations")
class CartServicePerformanceTest {

    private static final long MAX_LATENCY_MS = 500;
    private static final long MAX_CHECKOUT_MS = 1000;
    @Autowired
    private CartService cartService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryRepository categoryRepository;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private OrderProducer orderProducer;
    @MockitoBean
    private ProductProducer productProducer;

    // --- Helpers using Proper Record Parameters ---

    /**
     * Internal helper to generate a batch of products to facilitate cart testing.
     * * @param count The number of products to persist.
     *
     * @return A list of successfully created {@link ProductResponse} objects.
     */
    private List<ProductResponse> createTestProducts(int count) {
        Category category = categoryRepository.save(Category.builder()
                .name("Perf-Category-" + System.nanoTime())
                .build());

        List<ProductResponse> products = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            products.add(productService.createProduct(ProductCreateRequest.builder()
                    .name("Perf-Prod-" + i)
                    .price(BigDecimal.valueOf(9.99))
                    .stockQuantity(1000)
                    .categoryId(category.getId())
                    .brand("PerfBrand")
                    .imageUrls(List.of()) // Required by Record constructor
                    .build()));
        }
        return products;
    }

    /**
     * <b>Metric: Latency</b>
     * <p>Validates that adding a single item to a cart stays within the tight latency
     * requirements for a responsive UI.</p>
     */
    @Test
    @DisplayName("Latency: Add item to cart")
    void shouldAddItemToCartWithinAcceptableTime() {
        List<ProductResponse> products = createTestProducts(1);
        var request = AddToCartRequest.builder()
                .productId(products.get(0).id())
                .quantity(1).build();

        long start = System.currentTimeMillis();
        cartService.addItemToCart(1L, request);
        long duration = System.currentTimeMillis() - start;

        assertThat(duration).isLessThan(MAX_LATENCY_MS);
    }

    /**
     * <b>Metric: Throughput</b>
     * <p>Simulates a user rapidly adding 50 different items to a cart. Verifies
     * that the average time per addition remains consistent.</p>
     */
    @Test
    @DisplayName("Throughput: Sequential bulk additions")
    void shouldHandleBulkItemAdditionEfficiently() {
        int count = 50;
        List<ProductResponse> products = createTestProducts(count);
        long start = System.currentTimeMillis();

        for (ProductResponse p : products) {
            cartService.addItemToCart(3L, AddToCartRequest.builder()
                    .productId(p.id()).quantity(1).build());
        }

        long total = System.currentTimeMillis() - start;
        // Check average latency per item addition
        assertThat(total / count).isLessThan(MAX_LATENCY_MS);
    }

    /**
     * <b>Metric: Load/Complexity</b>
     * <p>Tests the checkout process with a heavily populated cart (100 items).
     * This evaluates the performance of the stream-based price summation and
     * the bulk clearing of cart items.</p>
     */
    @Test
    @DisplayName("Load: High-volume checkout")
    void shouldHandleLargeCartCheckoutEfficiently() {
        int itemCount = 100;
        List<ProductResponse> products = createTestProducts(itemCount);
        Long userId = 9L;

        // Fill the cart
        for (ProductResponse p : products) {
            cartService.addItemToCart(userId, AddToCartRequest.builder()
                    .productId(p.id()).quantity(1).build());
        }

        // Measure checkout (Calculations + Kafka Event + DB Update)
        long start = System.currentTimeMillis();
        cartService.checkout(userId);
        long duration = System.currentTimeMillis() - start;

        assertThat(duration).isLessThan(MAX_CHECKOUT_MS);

        // Assert state side effect
        assertThat(cartService.getCartByUserId(userId).items()).isEmpty();
    }
}