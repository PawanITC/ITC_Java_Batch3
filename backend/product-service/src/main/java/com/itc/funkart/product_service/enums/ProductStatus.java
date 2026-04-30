package com.itc.funkart.product_service.enums;

/**
 * Represents the visibility and availability state of a product in the store.
 */
public enum ProductStatus {
    /**
     * Product is being prepared and is not visible to customers.
     */
    DRAFT,
    /**
     * Product is live and available for purchase.
     */
    ACTIVE,
    /**
     * Product is temporarily hidden or disabled.
     */
    INACTIVE,
    /**
     * Product is no longer sold but retained for historical order data.
     */
    ARCHIVED
}