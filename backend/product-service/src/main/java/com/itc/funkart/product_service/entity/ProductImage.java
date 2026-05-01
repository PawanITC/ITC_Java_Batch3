package com.itc.funkart.product_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a media asset (URL) linked to a product.
 */
@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    /**
     * Unique identifier for the image record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Fully qualified URL or path to the image stored in S3 or CDN.
     */
    @Column(nullable = false)
    private String imageUrl;

    /**
     * Indicates if this image is the main thumbnail used in product listings.
     * Defaulted to false via @Builder.Default.
     */
    @Builder.Default
    private Boolean isPrimary = false;

    /**
     * The product this image belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Product product;
}