package com.itc.funkart.product_service.serviceImpl;

import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.request.ProductUpdateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.dto.response.ProductsResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.exceptions.BadRequestException;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.mapper.ProductMapper;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductCreateRequest createProductRequest(String name, Long categoryId) {
        return ProductCreateRequest.builder()
                .name(name)
                .description("Product description")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(categoryId)
                .brand("TestBrand")
                .build();
    }

    private Product createProduct(Long id, String name, Long categoryId) {
        return Product.builder()
                .id(id)
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .description("Product description")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .brand("TestBrand")
                .active(true)
                .category(Category.builder().id(categoryId).build())
                .build();
    }

    private Category createCategory(Long id, String name) {
        return Category.builder()
                .id(id)
                .name(name)
                .description("Category description")
                .build();
    }

    @Test
    @DisplayName("Should create product with valid category")
    void shouldCreateProductWithValidCategory() {
        // Arrange
        ProductCreateRequest request = createProductRequest("Laptop", 1L);
        Category category = createCategory(1L, "Electronics");
        Product product = createProduct(null, "Laptop", 1L);
        Product savedProduct = createProduct(1L, "Laptop", 1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // Act
        ProductResponse response = productService.createProduct(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Laptop");

        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when category not found during product creation")
    void shouldThrowExceptionWhenCategoryNotFoundDuringCreation() {
        // Arrange
        ProductCreateRequest request = createProductRequest("Laptop", 999L);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");

        verify(categoryRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get product by id")
    void shouldGetProductById() {
        // Arrange
        Product product = createProduct(1L, "Phone", 1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act
        ProductResponse response = productService.getProduct(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Phone");

        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        // Arrange
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should get all products")
    void shouldGetAllProducts() {
        // Arrange
        Product product1 = createProduct(1L, "Laptop", 1L);
        Product product2 = createProduct(2L, "Phone", 1L);
        List<Product> products = List.of(product1, product2);

        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(products);

        // Act
        List<ProductResponse> responses = productService.getAllProducts();

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("Laptop");
        assertThat(responses.get(1).getName()).isEqualTo("Phone");

        verify(productRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Should return empty list when no products exist")
    void shouldReturnEmptyListWhenNoProductsExist() {
        // Arrange
        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        // Act
        List<ProductResponse> responses = productService.getAllProducts();

        // Assert
        assertThat(responses).isEmpty();

        verify(productRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Should update product successfully")
    void shouldUpdateProduct() {
        // Arrange
        Product existingProduct = createProduct(1L, "Laptop", 1L);
        ProductUpdateRequest updateRequest = new ProductUpdateRequest();
        updateRequest.setName("Updated Laptop");
        updateRequest.setPrice(BigDecimal.valueOf(1299.99));

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        // Act
        ProductResponse response = productService.updateProduct(1L, updateRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);

        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent product")
    void shouldThrowExceptionWhenUpdatingNonExistentProduct() {
        // Arrange
        ProductUpdateRequest updateRequest = new ProductUpdateRequest();
        updateRequest.setName("Updated Product");

        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.updateProduct(999L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete product successfully")
    void shouldDeleteProduct() {
        // Arrange
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepository, times(1)).existsById(1L);
        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent product")
    void shouldThrowExceptionWhenDeletingNonExistentProduct() {
        // Arrange
        when(productRepository.existsById(anyLong())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository, times(1)).existsById(999L);
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should get products by ids")
    void shouldGetProductsByIds() {
        // Arrange
        Product product1 = createProduct(1L, "Laptop", 1L);
        Product product2 = createProduct(2L, "Phone", 1L);
        List<Long> ids = List.of(1L, 2L);

        when(productRepository.findAllById(ids)).thenReturn(List.of(product1, product2));

        // Act
        ProductsResponse response = productService.getProductsByIds(ids);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getFound()).hasSize(2);
        assertThat(response.getMissing()).isEmpty();

        verify(productRepository, times(1)).findAllById(ids);
    }

    @Test
    @DisplayName("Should throw exception when id list is empty")
    void shouldThrowExceptionWhenIdListIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> productService.getProductsByIds(List.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception when id list exceeds max size")
    void shouldThrowExceptionWhenIdListExceedsMaxSize() {
        // Arrange
        List<Long> ids = java.util.stream.LongStream.range(1, 2002)
                .boxed()
                .toList();

        // Act & Assert
        assertThatThrownBy(() -> productService.getProductsByIds(ids))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Too many IDs");

        verify(productRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("Should handle missing products in batch fetch")
    void shouldHandleMissingProductsInBatchFetch() {
        // Arrange
        Product product1 = createProduct(1L, "Laptop", 1L);
        List<Long> requestedIds = List.of(1L, 2L, 3L);

        when(productRepository.findAllById(requestedIds)).thenReturn(List.of(product1));

        // Act
        ProductsResponse response = productService.getProductsByIds(requestedIds);

        // Assert
        assertThat(response.getFound()).hasSize(1);
        assertThat(response.getMissing()).hasSize(2);
        assertThat(response.getMissing()).containsExactlyInAnyOrder(2L, 3L);

        verify(productRepository, times(1)).findAllById(requestedIds);
    }
}

