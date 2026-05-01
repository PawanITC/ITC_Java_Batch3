package com.itc.funkart.product_service.repository;

import com.itc.funkart.product_service.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link CartItem} entity operations.
 * <p>
 * Note: Prefer managing CartItems through the {@link CartRepository}
 * to ensure consistency within the cart aggregate.
 * </p>
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}