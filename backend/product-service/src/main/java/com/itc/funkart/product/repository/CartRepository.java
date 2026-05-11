package com.itc.funkart.product.repository;

import com.itc.funkart.product.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for {@link Cart} entity operations.
 * Provides optimized data access methods to minimize database round-trips.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Retrieves a user's cart along with all associated items and product details.
     * <p>
     * Uses a 'LEFT JOIN FETCH' strategy to resolve the N+1 select problem for
     * both CartItems and their nested Product entities in a single query.
     * </p>
     *
     * @param userId The unique identifier of the user.
     * @return An {@link Optional} containing the hydrated Cart, or empty if not found.
     */
    @Query("SELECT DISTINCT c FROM Cart c " +
            "LEFT JOIN FETCH c.items i " +
            "LEFT JOIN FETCH i.product " +
            "WHERE c.userId = :userId")
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);

    Optional<Cart> findByUserId(Long userId);
}