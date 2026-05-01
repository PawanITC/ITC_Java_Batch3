package com.itc.funkart.product_service.dto.response;

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