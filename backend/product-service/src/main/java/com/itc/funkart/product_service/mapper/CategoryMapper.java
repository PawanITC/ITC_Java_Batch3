package com.itc.funkart.product_service.mapper;

import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.dto.request.CategoryResponse;
import com.itc.funkart.product_service.entity.Category;
import org.springframework.stereotype.Component;

public class CategoryMapper {
    public static Category toEntity(CategoryRequest request) {
        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public static CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
