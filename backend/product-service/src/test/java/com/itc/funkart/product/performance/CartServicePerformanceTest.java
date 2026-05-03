package com.itc.funkart.product.performance;

import com.itc.funkart.common.dto.auth.response.product.ProductResponse;
import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.product.config.JwtConfig;
import com.itc.funkart.product.dto.request.AddToCartRequest;
import com.itc.funkart.product.dto.request.ProductCreateRequest;
import com.itc.funkart.product.entity.Category;
import com.itc.funkart.product.kafka.producer.OrderProducer;
import com.itc.funkart.product.kafka.producer.ProductProducer;
import com.itc.funkart.product.repository.CategoryRepository;
import com.itc.funkart.product.service.CartService;
import com.itc.funkart.product.service.JwtService;
import com.itc.funkart.product.service.ProductService;
import com.itc.funkart.product.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

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
 * <b>Note:</b> Identity is manually injected into the SecurityContext to satisfy
 * the {@link SecurityUtils} type-check.
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
    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private JwtConfig jwtConfig;
    @MockitoBean
    private OrderProducer orderProducer;
    @MockitoBean
    private ProductProducer productProducer;

    /**
     * Manually populates the SecurityContext with the specific JwtUserDto
     * expected by the Service layer.
     */
    private void mockAuth(Long userId) {
        JwtUserDto principal = JwtUserDto.builder()
                .id(userId)
                .name("PerfTestUser")
                .email("perf@test.com")
                .role("ROLE_USER")
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

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
                    .imageUrls(List.of())
                    .build()));
        }
        entityManager.flush();
        entityManager.clear();
        return products;
    }

    @Test
    @DisplayName("Latency: Add item to cart")
    void shouldAddItemToCartWithinAcceptableTime() {
        mockAuth(1L);
        List<ProductResponse> products = createTestProducts(1);
        var request = AddToCartRequest.builder()
                .productId(products.get(0).id())
                .quantity(1).build();

        StopWatch sw = new StopWatch();
        sw.start();
        cartService.addItemToCart(request);
        entityManager.flush(); // Force DB hit
        sw.stop();

        assertThat(sw.getTotalTimeMillis()).isLessThan(MAX_LATENCY_MS);
    }

    @Test
    @DisplayName("Throughput: Sequential bulk additions")
    void shouldHandleBulkItemAdditionEfficiently() {
        mockAuth(3L);
        int count = 50;
        List<ProductResponse> products = createTestProducts(count);

        StopWatch sw = new StopWatch();
        sw.start();
        for (ProductResponse p : products) {
            cartService.addItemToCart(AddToCartRequest.builder()
                    .productId(p.id()).quantity(1).build());
            entityManager.flush();
        }
        sw.stop();

        assertThat(sw.getTotalTimeMillis() / count).isLessThan(MAX_LATENCY_MS);
    }

    @Test
    @DisplayName("Load: High-volume checkout")
    void shouldHandleLargeCartCheckoutEfficiently() {
        mockAuth(9L);
        int itemCount = 100;
        List<ProductResponse> products = createTestProducts(itemCount);

        for (ProductResponse p : products) {
            cartService.addItemToCart(AddToCartRequest.builder()
                    .productId(p.id()).quantity(1).build());
        }
        entityManager.flush();
        entityManager.clear();

        StopWatch sw = new StopWatch();
        sw.start();
        cartService.checkout();
        entityManager.flush();
        sw.stop();

        assertThat(sw.getTotalTimeMillis()).isLessThan(MAX_CHECKOUT_MS);

        entityManager.clear();
        assertThat(cartService.getCart().items()).isEmpty();
    }
}