package com.itc.funkart.product_service.mapper;

import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.dto.response.CategoryResponse;
import com.itc.funkart.product_service.entity.Category;

/**
 * <h2>CategoryMapper</h2>
 * <p>
 * Utility class for converting between Category entities and DTOs.
 * Uses the Builder pattern for both JPA entities and Java Records.
 * </p>
 */
public class CategoryMapper {

    /**
     * Maps a {@link CategoryRequest} DTO to a {@link Category} JPA entity.
     *
     * @param request the request containing category details.
     * @return a new Category entity.
     */
    public static Category toEntity(CategoryRequest request) {
        if (request == null) return null;
        return Category.builder()
                .name(request.name())
                .description(request.description())
                .build();
    }

    /**
     * Maps a {@link Category} JPA entity to a {@link CategoryResponse} record.
     *
     * @param category the persistent category entity.
     * @return a category response DTO.
     */
    public static CategoryResponse toResponse(Category category) {
        if (category == null) return null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}