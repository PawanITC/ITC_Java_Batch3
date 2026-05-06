package com.itc.funkart.common.dto.auth.response.category;

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