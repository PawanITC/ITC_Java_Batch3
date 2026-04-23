package com.itc.funkart.product_service.serviceImpl;

import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.dto.request.CategoryResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.mapper.CategoryMapper;
import com.itc.funkart.product_service.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private CategoryRequest createCategoryRequest(String name, String description) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);
        request.setDescription(description);
        return request;
    }

    private Category createCategory(Long id, String name, String description) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(description)
                .build();
    }

    @Test
    @DisplayName("Should create category successfully")
    void shouldCreateCategory() {
        // Arrange
        CategoryRequest request = createCategoryRequest("Electronics", "Electronic devices");
        Category category = createCategory(null, "Electronics", "Electronic devices");
        Category savedCategory = createCategory(1L, "Electronics", "Electronic devices");

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // Act
        CategoryResponse response = categoryService.createCategory(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Electronics");
        assertThat(response.getDescription()).isEqualTo("Electronic devices");

        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Should get all categories")
    void shouldGetAllCategories() {
        // Arrange
        Category category1 = createCategory(1L, "Electronics", "Electronic devices");
        Category category2 = createCategory(2L, "Clothing", "Apparel and clothing");
        List<Category> categories = List.of(category1, category2);

        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<CategoryResponse> responses = categoryService.getAllCategories();

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("Electronics");
        assertThat(responses.get(1).getName()).isEqualTo("Clothing");

        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no categories exist")
    void shouldReturnEmptyListWhenNoCategoriesExist() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(List.of());

        // Act
        List<CategoryResponse> responses = categoryService.getAllCategories();

        // Assert
        assertThat(responses).isEmpty();

        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get category by id")
    void shouldGetCategoryById() {
        // Arrange
        Category category = createCategory(1L, "Electronics", "Electronic devices");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // Act
        CategoryResponse response = categoryService.getCategoryById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Electronics");

        verify(categoryRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when category not found")
    void shouldThrowExceptionWhenCategoryNotFound() {
        // Arrange
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.getCategoryById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(categoryRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should delete category successfully")
    void shouldDeleteCategory() {
        // Arrange
        doNothing().when(categoryRepository).deleteById(anyLong());

        // Act
        categoryService.deleteCategory(1L);

        // Assert
        verify(categoryRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should create multiple categories")
    void shouldCreateMultipleCategories() {
        // Arrange
        CategoryRequest request1 = createCategoryRequest("Electronics", "Electronic devices");
        CategoryRequest request2 = createCategoryRequest("Books", "Books and literature");

        Category savedCategory1 = createCategory(1L, "Electronics", "Electronic devices");
        Category savedCategory2 = createCategory(2L, "Books", "Books and literature");

        when(categoryRepository.save(any(Category.class)))
                .thenReturn(savedCategory1)
                .thenReturn(savedCategory2);

        // Act
        CategoryResponse response1 = categoryService.createCategory(request1);
        CategoryResponse response2 = categoryService.createCategory(request2);

        // Assert
        assertThat(response1.getId()).isEqualTo(1L);
        assertThat(response2.getId()).isEqualTo(2L);

        verify(categoryRepository, times(2)).save(any(Category.class));
    }
}

