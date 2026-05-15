package com.itc.funkart.product_service.performance;

import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.request.ProductUpdateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.dto.response.ProductsResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.producer.OrderProducer;
import com.itc.funkart.product_service.producer.ProductProducer;
import com.itc.funkart.product_service.repository.CategoryRepository;
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
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>ProductServicePerformanceTest</h2>
 * <p>
 * This suite provides basic benchmarking for the Product Service catalog operations.
 * It focuses on latency, memory efficiency, and throughput under sequential load.
 * </p>
 * <p>
 * <b>Warning:</b> These tests are environment-dependent. While they validate logic
 * efficiency, actual millisecond results may vary based on CPU and CI/CD runner load.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Performance: Product Catalog")
class ProductServicePerformanceTest {

    private static final long MAX_LATENCY_MS = 500;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private JwtService jwtService;

    // Neutralizing Kafka producers to keep performance focused on DB/Service logic
    @MockitoBean
    private OrderProducer orderProducer;

    @MockitoBean
    private ProductProducer productProducer;

    /**
     * Measures the latency of batch product retrieval.
     * Includes a warm-up call to ensure JIT compilation doesn't skew initial results.
     */
    @Test
    @DisplayName("Latency: Batch fetch products by ID list")
    void shouldBatchFetchProductsWithinAcceptableTime() {
        Category cat = categoryRepository.save(Category.builder().name("Batch-Perf").build());
        List<Long> ids = LongStream.rangeClosed(1, 10).boxed().toList();

        // Warm up
        productService.getProductsByIds(ids);

        long start = System.currentTimeMillis();
        ProductsResponse response = productService.getProductsByIds(ids);
        long duration = System.currentTimeMillis() - start;

        assertThat(duration).isLessThan(MAX_LATENCY_MS * 2);
    }

    /**
     * Benchmarks partial updates using the ProductUpdateRequest DTO.
     * Ensures that the mapping and persistence layer handle updates efficiently.
     */
    @Test
    @DisplayName("Latency: Partial update using ProductUpdateRequest")
    void shouldUpdateProductWithinAcceptableTime() {
        Category cat = categoryRepository.save(Category.builder().name("Update-Perf").build());
        ProductResponse product = productService.createProduct(ProductCreateRequest.builder()
                .name("Old Name")
                .price(BigDecimal.TEN)
                .categoryId(cat.getId())
                .brand("B")
                .imageUrls(List.of())
                .build());

        var updateRequest = ProductUpdateRequest.builder()
                .name("New Name")
                .price(BigDecimal.valueOf(99.99))
                .build();

        long start = System.currentTimeMillis();
        productService.updateProduct(product.id(), updateRequest);
        long duration = System.currentTimeMillis() - start;

        assertThat(duration).isLessThan(MAX_LATENCY_MS);
    }

    /**
     * Evaluates the memory footprint during a bulk creation cycle.
     * Uses System.gc() to attempt to stabilize the baseline before measuring.
     */
    @Test
    @DisplayName("Efficiency: Memory usage during bulk product creation")
    void shouldVerifyMemoryEfficiency() {
        Category cat = categoryRepository.save(Category.builder().name("Mem-Test").build());
        Runtime runtime = Runtime.getRuntime();

        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        } // Wait for GC

        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        for (int i = 0; i < 100; i++) {
            productService.createProduct(ProductCreateRequest.builder()
                    .name("Prod-" + i)
                    .price(BigDecimal.ONE)
                    .categoryId(cat.getId())
                    .brand("B")
                    .imageUrls(List.of())
                    .build());
        }

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long used = memoryAfter - memoryBefore;

        // Threshold set to 50MB to account for Hibernate's 1st level cache and object proxies
        assertThat(used).isLessThan(50 * 1024 * 1024);
    }

    /**
     * Validates that the service can handle high-frequency sequential reads
     * without significant degradation.
     */
    @Test
    @DisplayName("Throughput: Rapid sequential retrieval")
    void shouldHandleRapidSequentialRequests() {
        Category cat = categoryRepository.save(Category.builder().name("Rapid").build());
        ProductResponse p = productService.createProduct(ProductCreateRequest.builder()
                .name("Rapid")
                .price(BigDecimal.ONE)
                .categoryId(cat.getId())
                .brand("B")
                .imageUrls(List.of())
                .build());

        long start = System.currentTimeMillis();
        for (int i = 0; i < 200; i++) {
            productService.getProduct(p.id());
        }
        long duration = System.currentTimeMillis() - start;

        // 200 requests should execute rapidly on a local H2 instance
        assertThat(duration).isLessThan(2000);
    }
}