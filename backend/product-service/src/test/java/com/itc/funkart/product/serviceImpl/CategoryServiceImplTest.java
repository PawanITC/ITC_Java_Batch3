package com.itc.funkart.product.serviceImpl;

import com.itc.funkart.common.dto.auth.response.category.CategoryResponse;
import com.itc.funkart.product.dto.request.CategoryRequest;
import com.itc.funkart.product.entity.Category;
import com.itc.funkart.product.exceptions.ResourceNotFoundException;
import com.itc.funkart.product.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>CategoryServiceImplTest</h2>
 * <p>
 * Verifies category management logic.
 * Updated to utilize Record Builders and accessor syntax.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Category Service Unit Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    // --- Helper Methods ---

    private CategoryRequest createCategoryRequest() {
        return CategoryRequest.builder()
                .name("Electronics")
                .description("Gadgets")
                .build();
    }

    private Category createCategory(Long id, String name, String description) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(description)
                .build();
    }

    // --- 1. CREATE TESTS ---

    @Test
    @DisplayName("Create Category - Should map request and save successfully")
    void shouldCreateCategorySuccessfully() {
        // Arrange
        CategoryRequest request = createCategoryRequest();
        Category savedCategory = createCategory(1L, "Electronics", "Gadgets");

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // Act
        CategoryResponse response = categoryService.createCategory(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Electronics");

        verify(categoryRepository).save(any(Category.class));
    }

    // --- 2. RETRIEVAL TESTS ---

    @Test
    @DisplayName("Get All - Should return list of category responses")
    void shouldGetAllCategories() {
        // Arrange
        List<Category> categories = List.of(
                createCategory(1L, "Electronics", "Desc"),
                createCategory(2L, "Clothing", "Desc")
        );

        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<CategoryResponse> responses = categoryService.getAllCategories();

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Electronics");
        assertThat(responses.get(1).name()).isEqualTo("Clothing");
    }

    @Test
    @DisplayName("Get By Id - Should return category or throw 404 (Branch: Not Found)")
    void shouldHandleCategoryNotFound() {
        // Arrange
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    @DisplayName("Get All - Branch: No categories present")
    void shouldReturnEmptyListWhenNoneExist() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<CategoryResponse> responses = categoryService.getAllCategories();

        assertThat(responses).isEmpty();
    }

    // --- 3. DELETE TESTS ---

    @Test
    @DisplayName("Delete Category - Should fetch and then delete entity")
    void shouldDeleteCategory() {
        // Arrange
        Long categoryId = 1L;
        Category category = Category.builder()
                .id(categoryId)
                .name("Electronics")
                .build();

        // 1. Stub the findById to return our category object
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // 2. Stub the delete method (not deleteById!)
        doNothing().when(categoryRepository).delete(any(Category.class));

        // Act
        categoryService.deleteCategory(categoryId);

        // Assert
        // Verify findById was called to check existence/fetch state
        verify(categoryRepository).findById(categoryId);

        // Verify the actual delete(entity) method was called
        verify(categoryRepository).delete(category);

        // Ensure deleteById was NOT the method used
        verify(categoryRepository, never()).deleteById(anyLong());
    }
}