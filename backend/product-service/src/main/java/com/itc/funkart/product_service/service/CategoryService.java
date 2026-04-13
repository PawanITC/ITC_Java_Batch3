package com.itc.funkart.product_service.service;
import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.dto.request.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    List<CategoryResponse> getAllCategories();
    CategoryResponse getCategoryById(Long id);
    void deleteCategory(Long id);
}
