package com.itc.funkart.product_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.product_service.config.SecurityConfig;
import com.itc.funkart.product_service.controller.admin.AdminCategoryController;
import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.service.CategoryService;
import com.itc.funkart.product_service.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>CategoryControllerTest</h2>
 * Validates public and administrative category logic.
 * Ensures strict security partitioning and error mapping.
 */
@WebMvcTest({CategoryController.class, AdminCategoryController.class})
@DisplayName("Category Controller Comprehensive Tests")
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    @DisplayName("GET /categories - Branch: Empty List")
    void shouldReturnEmptyArrayWhenNoCategories() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.message").value("Operation successful"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /categories/{id} - Branch: 404 Not Found")
    void getCategory_NotFound() throws Exception {
        when(categoryService.getCategoryById(99L))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(get("/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("POST /admin/categories - Branch: 400 Validation Failure")
    void createCategory_InvalidInput() throws Exception {
        CategoryRequest invalidRequest = new CategoryRequest("", "");

        mockMvc.perform(post("/admin/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    @DisplayName("DELETE /admin/categories/{id} - Branch: 403 Forbidden")
    void deleteCategory_Forbidden() throws Exception {
        mockMvc.perform(delete("/admin/categories/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("DELETE /admin/categories/{id} - Branch: 200 Success")
    void deleteCategory_Success() throws Exception {
        mockMvc.perform(delete("/admin/categories/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Resource successfully removed"));

        verify(categoryService).deleteCategory(1L);
    }
}