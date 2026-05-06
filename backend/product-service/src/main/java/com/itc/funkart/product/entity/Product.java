package com.itc.funkart.product.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The core entity representing a sellable item in the Funkart catalog.
 * Contains inventory details, pricing, and associations with categories and images.
 * Annotated with @Builder.Default to ensure default values (active, collections)
 * are respected during object construction via Lombok builders.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    /**
     * Unique identifier for the product.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Display name of the product.
     */
    @Column(nullable = false)
    private String name;

    /**
     * URL-friendly unique identifier for the product (e.g., "iphone-15-pro").
     */
    @Column(unique = true, nullable = false)
    private String slug;

    /**
     * Detailed information about the product.
     */
    @Column(columnDefinition = "TEXT", length = 1000)
    private String description;

    /**
     * Retail price, stored as BigDecimal for financial precision.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Current count of units available in the warehouse.
     */
    private Integer stockQuantity;

    /**
     * Flag indicating if the product is currently visible on the frontend.
     * Initialized to true via @Builder.Default.
     */
    @Builder.Default
    private Boolean active = true;

    /**
     * Manufacturer or brand name.
     */
    private String brand;

    /**
     * Collection of images associated with this product.
     * Initialized as an empty ArrayList via @Builder.Default.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    /**
     * The category this product belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnore
    private Category category;


    /**
     * Timestamp of when the product was first added to the DB.
     */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last time the product details were modified.
     */
    private LocalDateTime updatedAt;

    /**
     * Synchronizes the bidirectional relationship between Product and ProductImage.
     *
     * @param image The image to associate with this product.
     */
    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    /**
     * Sets the creation and initial update timestamps before persisting.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the modification timestamp before saving changes.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}