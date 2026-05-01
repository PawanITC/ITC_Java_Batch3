package com.itc.funkart.product_service.performance;

import com.itc.funkart.product_service.config.JwtConfig;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.request.ProductUpdateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.kafka.producer.OrderProducer;
import com.itc.funkart.product_service.kafka.producer.ProductProducer;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.service.JwtService;
import com.itc.funkart.product_service.service.ProductService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>ProductServicePerformanceTest</h2>
 * <p>
 * Provides high-precision benchmarking for the Product Service.
 * Unlike standard unit tests, this suite monitors:
 * <ul>
 *     <li><b>Execution Latency:</b> Using high-resolution timers to avoid wall-clock skew.</li>
 *     <li><b>Persistence Overhead:</b> Measuring DB interaction by clearing Hibernate's First-Level cache.</li>
 *     <li><b>Memory Stability:</b> Tracking allocation deltas during bulk operations.</li>
 * </ul>
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Performance: Product Catalog & Persistence")
class ProductServicePerformanceTest {

    private static final long MAX_LATENCY_MS = 500;

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
     * Measures batch fetch latency.
     * Uses a warm-up phase to trigger initial JIT compilation before measurement.
     */
    @Test
    @DisplayName("Latency: Batch fetch products by ID list")
    void shouldBatchFetchProductsWithinAcceptableTime() {
        categoryRepository.save(Category.builder().name("Batch-Perf").build());
        List<Long> ids = LongStream.rangeClosed(1, 10).boxed().toList();

        // Warm up phase: JIT compilation and initial class loading
        productService.getProductsByIds(ids);
        entityManager.clear();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        productService.getProductsByIds(ids);

        stopWatch.stop();
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(MAX_LATENCY_MS * 2);
    }

    /**
     * Benchmarks the DTO mapping and merge overhead during partial updates.
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

        entityManager.flush();
        entityManager.clear();

        var updateRequest = ProductUpdateRequest.builder()
                .name("New Name")
                .price(BigDecimal.valueOf(99.99))
                .build();

        StopWatch sw = new StopWatch();
        sw.start();
        productService.updateProduct(product.id(), updateRequest);
        sw.stop();

        assertThat(sw.getTotalTimeMillis()).isLessThan(MAX_LATENCY_MS);
    }

    /**
     * Evaluates memory efficiency by creating resources in batches.
     * Manually clears the EntityManager to prevent a memory leak within the Transactional context.
     */
    @Test
    @DisplayName("Efficiency: Memory usage during bulk product creation")
    void shouldVerifyMemoryEfficiency() {
        Category cat = categoryRepository.save(Category.builder().name("Mem-Test").build());
        Runtime runtime = Runtime.getRuntime();

        // Stabilize heap before measurement
        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        for (int i = 0; i < 100; i++) {
            productService.createProduct(ProductCreateRequest.builder()
                    .name("Prod-" + i)
                    .price(BigDecimal.ONE)
                    .categoryId(cat.getId())
                    .brand("B")
                    .imageUrls(List.of())
                    .build());

            // Senior logic: Clear persistence context periodically to simulate real request/response lifecycles
            if (i % 20 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        assertThat(memoryAfter - memoryBefore).isLessThan(50 * 1024 * 1024);
    }

    /**
     * Validates throughput for high-frequency reads.
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

        // CRITICAL FIX: Flush the creation to the database so ID '12' actually exists
        entityManager.flush();
        entityManager.clear();

        StopWatch sw = new StopWatch();
        sw.start();
        for (int i = 0; i < 200; i++) {
            productService.getProduct(p.id());
            entityManager.clear(); // Ensure we aren't just reading from memory
        }
        sw.stop();

        assertThat(sw.getTotalTimeMillis()).isLessThan(2000);
    }
}