package com.itc.funkart.product_service.mapper;

import com.itc.funkart.product_service.dto.events.ProductEvent;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.entity.ProductImage;
import com.itc.funkart.product_service.enums.ProductEventType;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ProductMapper {

    public static Product toEntity(ProductCreateRequest request) {
        if (request == null) {
            return null;
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .slug(generateSlug(request.getName()))
                .active(true)
                .brand(request.getBrand())
                .images(new ArrayList<>()) // Initialize list
                .build();

        // Handle Images: Ensure bi-directional link is set
        if (request.getImageUrls() != null) {
            request.getImageUrls().forEach(url -> {
                ProductImage img = ProductImage.builder()
                        .imageUrl(url)
                        .product(product) // This is crucial for JPA
                        .isPrimary(false)
                        .build();
                product.getImages().add(img);
            });
        }

        return product;
    }

    /**
     * Converts Entity to Response DTO for the API.
     */
    public static ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .active(product.getActive())
                .brand(product.getBrand())
                // Safely get category name if it exists
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                // Extract only the URLs for the response
                .imageUrls(product.getImages() != null ?
                        product.getImages().stream()
                                .map(ProductImage::getImageUrl)
                                .collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }

    /**
     * Basic slug generator (e.g., "Nike Air Max" -> "nike-air-max")
     */
    private static String generateSlug(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");
    }

    public static ProductEvent toEvent(Product product, ProductEventType eventType,long id) {
        return new ProductEvent(eventType, toResponse(product),id
        );
    }
}

