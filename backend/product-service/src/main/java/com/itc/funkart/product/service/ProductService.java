package com.itc.funkart.product.service;

import com.itc.funkart.common.dto.auth.response.product.ProductResponse;
import com.itc.funkart.common.dto.auth.response.product.ProductsResponse;
import com.itc.funkart.product.dto.request.ProductCreateRequest;
import com.itc.funkart.product.dto.request.ProductUpdateRequest;
import com.itc.funkart.product.exceptions.ResourceNotFoundException;

import java.util.List;

/**
 * Service interface for Product lifecycle and catalog management.
 * <p>
 * <b>Note on Distributed Consistency:</b> All write operations synchronize with
 * Kafka via Transactional Hooks. Events are only published upon a successful DB commit.
 * </p>
 */
public interface ProductService {

    /**
     * Persists a new product and publishes a creation event.
     * <p>
     * <b>Side Effects:</b> Evicts the 'product-service:products' cache.
     * </p>
     *
     * @param request The product details.
     * @return The persisted product DTO.
     */
    ProductResponse createProduct(ProductCreateRequest request);

    /**
     * Updates product attributes. Supports partial updates via null-checking.
     * <p>
     * <b>Optimization:</b> Uses Hibernate Proxies for Category linking to avoid
     * unnecessary SELECT queries.
     * </p>
     *
     * @param id      The ID of the product to update.
     * @param request DTO containing updated fields.
     * @return The updated product state.
     * @throws ResourceNotFoundException if the ID is invalid.
     */
    ProductResponse updateProduct(Long id, ProductUpdateRequest request);

    /**
     * Retrieves a single product.
     *
     * @param id The unique identifier.
     * @return Detailed product information.
     * @throws ResourceNotFoundException if not found.
     */
    ProductResponse getProduct(Long id);

    /**
     * Retrieves the entire active catalog ordered by recency.
     * <p>
     * <b>Caching:</b> This method is backed by a Redis/Spring cache. Cache miss
     * triggers a 'JOIN FETCH' query to hydrate images in a single round-trip.
     * </p>
     *
     * @return List of all available products.
     */
    List<ProductResponse> getAllProducts();

    /**
     * Retrieves the catalog filtered by optional search text and/or category.
     * Bypasses the full-catalog cache so that filter results are always fresh.
     *
     * @param search     Case-insensitive substring match on product name (nullable).
     * @param categoryId Exact category ID filter (nullable).
     * @return Filtered product list.
     */
    List<ProductResponse> getFilteredProducts(String search, Long categoryId);

    /**
     * Deletes a product and publishes a deletion event.
     *
     * @param id The ID of the product to remove.
     * @throws ResourceNotFoundException if the product does not exist.
     */
    void deleteProduct(Long id);

    /**
     * Performs a batch lookup of products.
     * <p>
     * This method is optimized for the Cart and Order services to resolve IDs
     * into displayable entities efficiently.
     * </p>
     *
     * @param ids List of product identifiers.
     * @return A response containing found entities and a list of missing IDs.
     */
    ProductsResponse getProductsByIds(List<Long> ids);
}