package com.itc.funkart.product_service.mapper;

import com.itc.funkart.product_service.dto.events.ProductEvent;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.entity.Product;

public class ProductMapper {

    public static ProductResponse toResponse(Product product) {

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .active(product.getActive())
                .build();
    }

    public static ProductEvent toEvent(Product product) {
        return new ProductEvent(
                "PRODUCT_CREATED",
                product.getId(),
                product.getName()
        );
    }
}

