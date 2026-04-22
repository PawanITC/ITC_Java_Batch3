package com.itc.funkart.product_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.product_service.dto.request.CategoryResponse;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@DisplayName("CategoryController Tests")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    private CategoryResponse createCategoryResponse(Long id, String name, String description) {
        return CategoryResponse.builder()
                .id(id)
                .name(name)
                .description(description)
                .build();
    }

    @Test
    @DisplayName("Should get all categories successfully")
    void shouldGetAllCategoriesSuccessfully() throws Exception {
        // Arrange
        CategoryResponse cat1 = createCategoryResponse(1L, "Electronics", "Electronic devices");
        CategoryResponse cat2 = createCategoryResponse(2L, "Clothing", "Apparel and clothing");
        List<CategoryResponse> categories = List.of(cat1, cat2);

        when(categoryService.getAllCategories()).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("Clothing"));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("Should return empty list when no categories exist")
    void shouldReturnEmptyListWhenNoCategoriesExist() throws Exception {
        // Arrange
        when(categoryService.getAllCategories()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("Should get category by id successfully")
    void shouldGetCategoryByIdSuccessfully() throws Exception {
        // Arrange
        CategoryResponse category = createCategoryResponse(1L, "Electronics", "Electronic devices");
        when(categoryService.getCategoryById(1L)).thenReturn(category);

        // Act & Assert
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.description").value("Electronic devices"));

        verify(categoryService, times(1)).getCategoryById(1L);
    }

    @Test
    @DisplayName("Should return 404 when category not found")
    void shouldReturn404WhenCategoryNotFound() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(anyLong()))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        // Act & Assert
        mockMvc.perform(get("/api/categories/999"))
                .andExpect(status().isNotFound());

        verify(categoryService, times(1)).getCategoryById(999L);
    }

    @Test
    @DisplayName("Should handle internal server error")
    void shouldHandleInternalServerError() throws Exception {
        // Arrange
        when(categoryService.getAllCategories())
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isInternalServerError());

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("Should return correct content type")
    void shouldReturnCorrectContentType() throws Exception {
        // Arrange
        CategoryResponse category = createCategoryResponse(1L, "Books", "Books and literature");
        when(categoryService.getCategoryById(1L)).thenReturn(category);

        // Act & Assert
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));

        verify(categoryService, times(1)).getCategoryById(1L);
    }

    @Test
    @DisplayName("Should handle multiple category requests")
    void shouldHandleMultipleCategoryRequests() throws Exception {
        // Arrange
        CategoryResponse cat1 = createCategoryResponse(1L, "Electronics", "Electronic devices");
        CategoryResponse cat2 = createCategoryResponse(2L, "Books", "Books and literature");

        when(categoryService.getCategoryById(1L)).thenReturn(cat1);
        when(categoryService.getCategoryById(2L)).thenReturn(cat2);

        // Act & Assert
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"));

        mockMvc.perform(get("/api/categories/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Books"));

        verify(categoryService, times(1)).getCategoryById(1L);
        verify(categoryService, times(1)).getCategoryById(2L);
    }

    @Test
    @DisplayName("Should return valid category id in response")
    void shouldReturnValidCategoryIdInResponse() throws Exception {
        // Arrange
        CategoryResponse category = createCategoryResponse(5L, "Furniture", "Home furniture");
        when(categoryService.getCategoryById(5L)).thenReturn(category);

        // Act & Assert
        mockMvc.perform(get("/api/categories/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L));

        verify(categoryService, times(1)).getCategoryById(5L);
    }
}

