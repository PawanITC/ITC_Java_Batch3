package com.itc.funkart.product.serviceImpl;

import com.itc.funkart.common.dto.auth.response.product.ProductResponse;
import com.itc.funkart.common.dto.auth.response.product.ProductsResponse;
import com.itc.funkart.product.dto.request.ProductCreateRequest;
import com.itc.funkart.product.dto.request.ProductUpdateRequest;
import com.itc.funkart.product.entity.Category;
import com.itc.funkart.product.entity.Product;
import com.itc.funkart.product.exceptions.BadRequestException;
import com.itc.funkart.product.exceptions.ResourceNotFoundException;
import com.itc.funkart.product.kafka.producer.ProductProducer;
import com.itc.funkart.product.repository.CategoryRepository;
import com.itc.funkart.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>ProductServiceImplTest</h2>
 * <p>
 * Comprehensive test suite for the Product Service.
 * Groups include Lifecycle (Create/Update), Retrieval, and Transactional integrity.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Product Service - Enterprise Behavior Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductProducer productProducer;

    @InjectMocks
    private ProductServiceImpl productService;

    @Nested
    @DisplayName("Product Lifecycle (Creation & Updates)")
    class Lifecycle {

        @Test
        @DisplayName("Create: Should map category proxy and register Kafka event")
        void createProductSuccess() {
            try (MockedStatic<TransactionSynchronizationManager> tsm = mockStatic(TransactionSynchronizationManager.class)) {
                // Arrange
                tsm.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
                ProductCreateRequest req = ProductCreateRequest.builder().name("Phone").categoryId(1L).build();
                Category proxy = Category.builder().id(1L).build();

                when(categoryRepository.getReferenceById(1L)).thenReturn(proxy);
                when(productRepository.save(any(Product.class))).thenAnswer(i -> {
                    Product p = i.getArgument(0);
                    p.setId(99L);
                    return p;
                });

                // Act
                ProductResponse resp = productService.createProduct(req);

                // Assert
                assertThat(resp.id()).isEqualTo(99L);
                verify(categoryRepository).getReferenceById(1L);

                // Capture and trigger the transaction sync manually
                ArgumentCaptor<TransactionSynchronization> syncCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);
                tsm.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));
                syncCaptor.getValue().afterCommit();
                verify(productProducer).sendMessage(any());
            }
        }

        @Test
        @DisplayName("Update: Should perform partial update (Ignoring nulls in request)")
        void updateProductPartial() {
            Product existing = Product.builder().id(1L).name("Original").brand("Apple").build();
            // Only updating name, brand remains "Apple"
            ProductUpdateRequest req = ProductUpdateRequest.builder().name("New").build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

            ProductResponse resp = productService.updateProduct(1L, req);

            assertThat(resp.name()).isEqualTo("New");
            // Verify brand was not wiped
            verify(productRepository).save(argThat(p -> p.getBrand().equals("Apple")));
        }
    }

    @Nested
    @DisplayName("Edge Case: Error Handling & Validations")
    class EdgeCases {

        @Test
        @DisplayName("Proxy Failure: Should convert EntityNotFound to ResourceNotFound")
        void handleMissingCategoryProxy() {
            ProductCreateRequest req = ProductCreateRequest.builder().categoryId(404L).build();
            when(categoryRepository.getReferenceById(404L)).thenThrow(EntityNotFoundException.class);

            assertThatThrownBy(() -> productService.createProduct(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found");
        }

        @Test
        @DisplayName("Batch Get: Should throw BadRequest on null/empty input")
        void batchEmptyInput() {
            assertThatThrownBy(() -> productService.getProductsByIds(null))
                    .isInstanceOf(BadRequestException.class);

            assertThatThrownBy(() -> productService.getProductsByIds(Collections.emptyList()))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Delete: Should fail fast if product doesn't exist")
        void deleteMissingProduct() {
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProduct(1L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(productRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Retrieval Logic")
    class Retrieval {
        @Test
        @DisplayName("Batch: Should identify missing IDs from requested list")
        void batchLookupWithMissing() {
            List<Long> requested = List.of(1L, 2L);
            Product p1 = Product.builder().id(1L).name("P1").build();

            when(productRepository.findAllById(any())).thenReturn(List.of(p1));

            ProductsResponse resp = productService.getProductsByIds(requested);

            assertThat(resp.found()).hasSize(1);
            assertThat(resp.missing()).containsExactly(2L);
        }
    }
}