package com.itc.funkart.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent entity representing a User's shopping cart.
 * Each cart is uniquely tied to a specific User ID from the User Service.
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    /**
     * Unique identifier for the cart record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * The ID of the user who owns this cart. Cross-referenced from the User Service.
     */
    @Column(nullable = false, unique = true)
    private Long userId;

    /**
     * Collection of items currently in the cart.
     * Initialized to an empty ArrayList to prevent NullPointerExceptions during building.
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();


    public void addCartItem(CartItem item) {
        this.items.add(item);
        item.setCart(this); // CRITICAL: Sets the back-reference for Hibernate
    }
}