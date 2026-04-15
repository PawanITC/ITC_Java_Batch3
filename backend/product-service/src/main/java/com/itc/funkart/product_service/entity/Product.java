package com.itc.funkart.product_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    //@Column(columnDefinition = "TEXT")
    @Column(length = 1000)
    private String description;

    @Column(precision = 10, scale = 2)
    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal price;

    private Integer stockQuantity;

    private Boolean active = true;

    //@Column(nullable=false)
    private String brand;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnore
    private Category category;

    // ADDED: Relationship to CartItem
    @OneToMany(mappedBy = "product")
    private List<CartItem> cartItems;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Helper method to keep bi-directional relationship in sync
    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
