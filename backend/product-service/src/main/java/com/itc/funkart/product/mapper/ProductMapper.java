package com.itc.funkart.product.mapper;

import com.itc.funkart.common.dto.auth.response.product.ProductResponse;
import com.itc.funkart.common.dto.event.product.ProductEvent;
import com.itc.funkart.common.enums.product.ProductEventType;
import com.itc.funkart.product.dto.request.ProductCreateRequest;
import com.itc.funkart.product.entity.Product;
import com.itc.funkart.product.entity.ProductImage;

import java.util.ArrayList;

/**
 * <h2>ProductMapper</h2>
 * <p>
 * Centralized mapping for Product lifecycle objects.
 * Handles entity creation, response projection, and Kafka event generation.
 * </p>
 */
public class ProductMapper {

    /**
     * Converts a {@link ProductCreateRequest} into a {@link Product} entity.
     * Automatically generates a slug and initializes the image collection.
     *
     * @param request the creation request from the client.
     * @return an initialized Product entity.
     */
    public static Product toEntity(ProductCreateRequest request) {
        if (request == null) return null;

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .slug(generateSlug(request.name()))
                .active(true)
                .brand(request.brand())
                .images(new ArrayList<>())
                .build();

        if (request.imageUrls() != null) {
            request.imageUrls().forEach(url -> product.addImage(
                    ProductImage.builder().imageUrl(url).isPrimary(false).build()
            ));
        }
        return product;
    }

    /**
     * Projects a {@link Product} entity onto a {@link ProductResponse} DTO.
     *
     * @param product the managed product entity.
     * @return a serialized product response.
     */
    public static ProductResponse toResponse(Product product) {
        if (product == null) return null;

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .imageUrls(product.getImages().stream().map(ProductImage::getImageUrl).toList())
                .active(product.getActive())
                .brand(product.getBrand())
                .build();
    }

    /**
     * Wraps a product into a {@link ProductEvent} for cross-service communication via Kafka.
     *
     * @param product the affected product.
     * @param type    the nature of the change (CREATE, UPDATE, DELETE).
     * @param id      the primary key of the product.
     * @return a Kafka-ready event DTO.
     */
    public static ProductEvent toEvent(Product product, ProductEventType type, long id) {
        return ProductEvent.builder()
                .eventType(type)
                .product(toResponse(product))
                .id(id)
                .build();
    }

    private static String generateSlug(String name) {
        if (name == null) return "";
        return name.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-");
    }
}