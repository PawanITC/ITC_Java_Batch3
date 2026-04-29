package com.itc.funkart.product_service.dto.response;

import lombok.Builder;

/**
 * standard response for category information.
 */
@Builder
public record CategoryResponse(
        Long id,
        String name,
        String description
) {
}