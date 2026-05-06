package com.itc.funkart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * <h2>OrderItem Entity</h2>
 * <p>
 * Represents a historical snapshot of a product purchase.
 * Storing the priceAtPurchase ensures the order remains accurate
 * even if the Product Service updates catalog prices later.
 * </p>
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * Snapshot price per unit at the moment the order was placed.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal priceAtPurchase;
}