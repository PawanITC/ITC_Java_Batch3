package com.itc.funkart.product_service.service;

import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.request.ProductUpdateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.dto.response.ProductsResponse;

import java.util.List;

/**
 * Service interface for managing the Product catalog.
 * Handles core business logic including creation, updates, and inventory tracking.
 */
public interface ProductService {

    /**
     * Creates a new product in the system and publishes a creation event.
     * * @param request The product details and image URLs.
     *
     * @return The created product details.
     */
    ProductResponse createProduct(ProductCreateRequest request);

    /**
     * Updates an existing product's details. Supports partial updates.
     * * @param id The unique identifier of the product to update.
     *
     * @param request The updated fields.
     * @return The updated product state.
     */
    ProductResponse updateProduct(Long id, ProductUpdateRequest request);

    /**
     * Retrieves a single product by its ID.
     * * @param id The unique identifier.
     *
     * @return The product details.
     * @throws com.itc.funkart.product_service.exceptions.ResourceNotFoundException if not found.
     */
    ProductResponse getProduct(Long id);

    /**
     * Retrieves all products, ordered by creation date (descending).
     * Typically cached for performance.
     * * @return List of all available products.
     */
    List<ProductResponse> getAllProducts();

    /**
     * Removes a product from the system and triggers a delete event for downstream services.
     * * @param id The ID of the product to remove.
     */
    void deleteProduct(Long id);

    /**
     * Batch retrieves products by a list of IDs.
     *
     * @param ids List of product identifiers.
     * @return A response containing found products and a list of any missing IDs.
     */
    ProductsResponse getProductsByIds(List<Long> ids);
}