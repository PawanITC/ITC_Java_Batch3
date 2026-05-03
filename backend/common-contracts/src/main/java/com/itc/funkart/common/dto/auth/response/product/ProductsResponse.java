package com.itc.funkart.common.dto.auth.response.product;

import lombok.Builder;

import java.util.List;

/**
 * Wraps a bulk product query response, identifying missing IDs.
 * Annotated with @Builder for clean instantiation in service logic and tests.
 */
@Builder
public record ProductsResponse(
        List<ProductResponse> found,
        List<Long> missing
) {
}
