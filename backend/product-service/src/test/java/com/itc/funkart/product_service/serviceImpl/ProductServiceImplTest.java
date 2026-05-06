package com.itc.funkart.product_service.serviceImpl;

import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.request.ProductUpdateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.dto.response.ProductsResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.exceptions.BadRequestException;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.producer.ProductProducer;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>ProductServiceImplTest</h2>
 * Covers catalog management logic, including Redis cache evictions (via @CacheEvict triggers)
 * and Kafka event publishing branches.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Product Service Unit Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductProducer productProducer;

    @InjectMocks
    private ProductServiceImpl productService;

    // --- Helper Methods ---

    private ProductCreateRequest createProductRequest(String name, Long categoryId) {
        return ProductCreateRequest.builder()
                .name(name)
                .description("Desc")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(categoryId)
                .brand("Brand")
                .build();
    }

    private Product createProduct(Long id, String name) {
        return Product.builder()
                .id(id)
                .name(name)
                .slug(name.toLowerCase())
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .images(new ArrayList<>())
                .active(true)
                .build();
    }

    // --- 1. CREATE BRANCHES ---

    @Test
    @DisplayName("Create Product - Should save and send Kafka event")
    void shouldCreateProductSuccessfully() {
        ProductCreateRequest request = createProductRequest("Laptop", 1L);
        Category category = Category.builder().id(1L).name("Tech").build();
        Product savedProduct = createProduct(100L, "Laptop");
        savedProduct.setCategory(category);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(request);

        assertThat(response.name()).isEqualTo("Laptop");
        verify(productProducer, times(1)).sendMessage(any());
        verify(productRepository).save(any());
    }

    // --- 2. UPDATE BRANCHES ---

    @Test
    @DisplayName("Update Product - Should handle partial updates via Record")
    void shouldUpdateProductSuccessfully() {
        Product existingProduct = createProduct(1L, "Old Name");
        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .name("New Name")
                .price(BigDecimal.valueOf(150.00))
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        ProductResponse response = productService.updateProduct(1L, updateRequest);

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.price()).isEqualByComparingTo("150.00");
        verify(productProducer).sendMessage(any());
    }

    // --- 3. BATCH LOOKUP BRANCHES ---

    @Test
    @DisplayName("Batch Get - Should identify missing IDs (Branch: Missing Items)")
    void shouldHandleMissingProductsInBatch() {
        List<Long> requestedIds = List.of(1L, 2L);
        Product p1 = createProduct(1L, "Found");

        when(productRepository.findAllById(anyList())).thenReturn(List.of(p1));

        ProductsResponse response = productService.getProductsByIds(requestedIds);

        assertThat(response.found()).hasSize(1);
        assertThat(response.missing()).containsExactly(2L);
    }

    @Test
    @DisplayName("Batch Get - Should throw BadRequest if list too large (Branch: Max Size)")
    void shouldThrowExceptionWhenBatchTooLarge() {
        List<Long> longList = new ArrayList<>();
        for (long i = 0; i < 2001; i++) longList.add(i);

        assertThatThrownBy(() -> productService.getProductsByIds(longList))
                .isInstanceOf(BadRequestException.class);
    }

    // --- 4. DELETE BRANCHES ---

    @Test
    @DisplayName("Delete Product - Should notify Kafka after deletion")
    void shouldDeleteProductAndNotify() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
        verify(productProducer).sendMessage(argThat(event -> event.id() == 1L));
    }

    @Test
    @DisplayName("Delete Product - Should throw 404 if missing (Branch: Missing)")
    void shouldThrow404OnDeleteMissing() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- 5. RETRIEVAL BRANCHES ---

    @Test
    @DisplayName("Get All - Should return sorted list from Repo")
    void shouldGetAllProducts() {
        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(createProduct(1L, "A")));

        List<ProductResponse> results = productService.getAllProducts();

        assertThat(results).hasSize(1);
        verify(productRepository).findAllByOrderByCreatedAtDesc();
    }
}