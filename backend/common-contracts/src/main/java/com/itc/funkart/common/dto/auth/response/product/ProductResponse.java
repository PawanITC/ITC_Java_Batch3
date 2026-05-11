package com.itc.funkart.common.dto.auth.response.product;

import lombok.Builder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Standard API response for product details.
 *
 * @param id           Database primary key of the product.
 * @param name         Display name shown in the catalogue.
 * @param slug         URL-friendly identifier (e.g., {@code "retro-city-print"}).
 * @param description  Full product description.
 * @param price        Retail price in the platform's base currency.
 * @param categoryName Name of the product's parent category.
 * @param imageUrls    Ordered list of publicly accessible image URLs.
 * @param active       Whether the product is visible and purchasable.
 * @param brand        Brand or artist attribution label.
 */
@Builder
public record ProductResponse(
        Long id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        String categoryName,
        List<String> imageUrls,
        Boolean active,
        String brand
) implements Serializable {
}
