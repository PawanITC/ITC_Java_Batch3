package com.itc.funkart.product_service.performance;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.service.CartService;
import com.itc.funkart.product_service.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Cart Performance Tests")
class CartServicePerformanceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    private static final long MAX_RESPONSE_TIME_MS = 500;
    private static final long MAX_CHECKOUT_TIME_MS = 1000;

    private List<ProductResponse> createTestProducts(int count) {
        Category category = categoryRepository.save(Category.builder()
                .name("Performance Test Category")
                .description("Test")
                .build());

        List<ProductResponse> products = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ProductResponse product = productService.createProduct(ProductCreateRequest.builder()
                    .name("Product " + i)
                    .description("Test")
                    .price(BigDecimal.valueOf(99.99))
                    .stockQuantity(1000)
                    .categoryId(category.getId())
                    .brand("Brand")
                    .build());
            products.add(product);
        }
        return products;
    }

    @Test
    @DisplayName("Should add item to cart within acceptable response time")
    void shouldAddItemToCartWithinAcceptableTime() {
        // Arrange
        List<ProductResponse> products = createTestProducts(1);
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(products.get(0).getId());
        request.setQuantity(1);

        // Act
        long startTime = System.currentTimeMillis();
        cartService.addItemToCart(1L, request);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(duration).isLessThan(MAX_RESPONSE_TIME_MS)
                .as("Add to cart should complete within %dms, but took %dms",
                    MAX_RESPONSE_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should retrieve cart within acceptable response time")
    void shouldRetrieveCartWithinAcceptableTime() {
        // Arrange
        List<ProductResponse> products = createTestProducts(5);
        for (ProductResponse product : products) {
            AddToCartRequest request = new AddToCartRequest();
            request.setProductId(product.getId());
            request.setQuantity(1);
            cartService.addItemToCart(2L, request);
        }

        // Act
        long startTime = System.currentTimeMillis();
        cartService.getCartByUserId(2L);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(duration).isLessThan(MAX_RESPONSE_TIME_MS)
                .as("Get cart should complete within %dms, but took %dms",
                    MAX_RESPONSE_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should handle bulk item addition efficiently")
    void shouldHandleBulkItemAdditionEfficiently() {
        // Arrange
        List<ProductResponse> products = createTestProducts(50);

        long startTime = System.currentTimeMillis();

        // Act
        for (ProductResponse product : products) {
            AddToCartRequest request = new AddToCartRequest();
            request.setProductId(product.getId());
            request.setQuantity(1);
            cartService.addItemToCart(3L, request);
        }

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        long averagePerItem = totalDuration / products.size();

        // Assert
        assertThat(averagePerItem).isLessThan(MAX_RESPONSE_TIME_MS)
                .as("Average add time should be under %dms, but was %dms",
                    MAX_RESPONSE_TIME_MS, averagePerItem);
    }

    @Test
    @DisplayName("Should checkout efficiently")
    void shouldCheckoutEfficiently() {
        // Arrange
        List<ProductResponse> products = createTestProducts(10);
        for (ProductResponse product : products) {
            AddToCartRequest request = new AddToCartRequest();
            request.setProductId(product.getId());
            request.setQuantity(1);
            cartService.addItemToCart(4L, request);
        }

        // Act
        long startTime = System.currentTimeMillis();
        cartService.checkout(4L);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(duration).isLessThan(MAX_CHECKOUT_TIME_MS)
                .as("Checkout should complete within %dms, but took %dms",
                    MAX_CHECKOUT_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should measure throughput of add to cart operations")
    void shouldMeasureThroughputOfAddToCartOperations() {
        // Arrange
        List<ProductResponse> products = createTestProducts(100);
        int operationCount = products.size();

        long startTime = System.currentTimeMillis();

        // Act
        for (int i = 0; i < operationCount; i++) {
            AddToCartRequest request = new AddToCartRequest();
            request.setProductId(products.get(i).getId());
            request.setQuantity(1);
            cartService.addItemToCart(5L, request);
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double throughput = (operationCount * 1000.0) / totalTime;

        // Assert
        assertThat(throughput).isGreaterThan(10.0)
                .as("Throughput should be greater than 10 ops/sec, but was %.2f",
                    throughput);
    }

    @Test
    @DisplayName("Should handle multiple concurrent users efficiently")
    void shouldHandleMultipleConcurrentUsersEfficiently() {
        // Arrange
        List<ProductResponse> products = createTestProducts(10);

        long startTime = System.currentTimeMillis();

        // Act - Simulate 20 users adding items
        for (int userId = 100; userId < 120; userId++) {
            for (ProductResponse product : products) {
                AddToCartRequest request = new AddToCartRequest();
                request.setProductId(product.getId());
                request.setQuantity(1);
                cartService.addItemToCart((long) userId, request);
            }
        }

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        double operationsPerSecond = (20 * 10 * 1000.0) / totalDuration;

        // Assert
        assertThat(operationsPerSecond).isGreaterThan(5.0)
                .as("Should handle at least 5 ops/sec with multiple users");
    }

    @Test
    @DisplayName("Should update item quantity efficiently")
    void shouldUpdateItemQuantityEfficiently() {
        // Arrange
        List<ProductResponse> products = createTestProducts(1);
        AddToCartRequest addRequest = new AddToCartRequest();
        addRequest.setProductId(products.get(0).getId());
        addRequest.setQuantity(5);
        cartService.addItemToCart(6L, addRequest);

        com.itc.funkart.product_service.dto.request.CartItemUpdateDto updateDto = 
                new com.itc.funkart.product_service.dto.request.CartItemUpdateDto();
        updateDto.setQuantityChange(3);

        // Act
        long startTime = System.currentTimeMillis();
        cartService.updateItemQuantity(6L, products.get(0).getId(), updateDto);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(duration).isLessThan(MAX_RESPONSE_TIME_MS)
                .as("Update should complete within %dms, but took %dms",
                    MAX_RESPONSE_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should remove item efficiently")
    void shouldRemoveItemEfficiently() {
        // Arrange
        List<ProductResponse> products = createTestProducts(5);
        for (ProductResponse product : products) {
            AddToCartRequest request = new AddToCartRequest();
            request.setProductId(product.getId());
            request.setQuantity(1);
            cartService.addItemToCart(7L, request);
        }

        // Act
        long startTime = System.currentTimeMillis();
        cartService.removeItemsFromCart(7L, products.get(0).getId());
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(duration).isLessThan(MAX_RESPONSE_TIME_MS)
                .as("Remove should complete within %dms, but took %dms",
                    MAX_RESPONSE_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should clear cart efficiently")
    void shouldClearCartEfficiently() {
        // Arrange
        List<ProductResponse> products = createTestProducts(20);
        for (ProductResponse product : products) {
            AddToCartRequest request = new AddToCartRequest();
            request.setProductId(product.getId());
            request.setQuantity(1);
            cartService.addItemToCart(8L, request);
        }

        // Act
        long startTime = System.currentTimeMillis();
        cartService.clearCart(8L);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(duration).isLessThan(MAX_RESPONSE_TIME_MS)
                .as("Clear cart should complete within %dms, but took %dms",
                    MAX_RESPONSE_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should handle large cart checkout efficiently")
    void shouldHandleLargeCartCheckoutEfficiently() {
        // Arrange
        List<ProductResponse> products = createTestProducts(100);
        for (int i = 0; i < Math.min(100, products.size()); i++) {
            AddToCartRequest request = new AddToCartRequest();
            request.setProductId(products.get(i).getId());
            request.setQuantity(1);
            cartService.addItemToCart(9L, request);
        }

        // Act
        long startTime = System.currentTimeMillis();
        cartService.checkout(9L);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertThat(duration).isLessThan(MAX_CHECKOUT_TIME_MS)
                .as("Large checkout should complete within %dms, but took %dms",
                    MAX_CHECKOUT_TIME_MS, duration);
    }

    @Test
    @DisplayName("Should verify response time consistency")
    void shouldVerifyResponseTimeConsistency() {
        // Arrange
        List<ProductResponse> products = createTestProducts(10);
        List<Long> responseTimes = new ArrayList<>();

        // Act
        for (int i = 0; i < 10; i++) {
            AddToCartRequest request = new AddToCartRequest();
            request.setProductId(products.get(i % products.size()).getId());
            request.setQuantity(1);

            long startTime = System.currentTimeMillis();
            cartService.addItemToCart(10L, request);
            long endTime = System.currentTimeMillis();
            responseTimes.add(endTime - startTime);
        }

        // Assert
        long averageTime = (long) responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        assertThat(averageTime).isLessThan(MAX_RESPONSE_TIME_MS);
        assertThat(maxTime).isLessThan((long)(MAX_RESPONSE_TIME_MS * 1.5)) // Allow 50% variance
                .as("Max response time should be reasonable, but was %dms", maxTime);
    }
}

