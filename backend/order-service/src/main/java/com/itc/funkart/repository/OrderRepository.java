package com.itc.funkart.repository;

import com.itc.funkart.entity.Order;
import com.itc.funkart.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Retrieves all orders associated with a specific customer.
     * Use this for the "My Orders" page on the frontend.
     * * @param customerId The 'sub' extracted from the JWT.
     *
     * @return A list of orders sorted by most recent first.
     */
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /**
     * Optional: Find a specific order only if it belongs to the customer.
     * Prevents User A from viewing User B's order by guessing the ID.
     */
    java.util.Optional<Order> findByIdAndCustomerId(Long id, Long customerId);

    /**
     * Find a list of orders by their order status.
     * Will use pagination to prevent pulling unnecessary amount.
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}

