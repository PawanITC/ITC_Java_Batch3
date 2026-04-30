package com.itc.funkart.mapper;

import com.itc.funkart.dto.OrderItemRequest;
import com.itc.funkart.dto.OrderRequest;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.Order;
import com.itc.funkart.entity.OrderItem;
import com.itc.funkart.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>OrderMapperTest</h2>
 * <p>
 * Verifies that the manual mapping logic correctly transforms data between
 * API DTOs and Database Entities. This is critical for data integrity.
 * </p>
 */
class OrderMapperTest {

    private OrderMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderMapper();
    }

    @Test
    @DisplayName("toEntity - Should map request with items to order entity")
    void toEntity_shouldMapCorrectly() {

        // Arrange
        OrderItemRequest itemReq = OrderItemRequest.builder()
                .productId(10L)
                .quantity(2)
                .build();

        OrderRequest request = new OrderRequest();
        request.setItems(List.of(itemReq));

        // Act
        Order result = mapper.toEntity(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        OrderItem item = result.getItems().get(0);

        assertEquals(10L, item.getProductId());
        assertEquals(2, item.getQuantity());

        // ✔ This is the ONLY valid relationship check at mapper level
        assertEquals(result, item.getOrder());
    }

    @Test
    @DisplayName("toResponse - Should map entity fields to response DTO")
    void toResponse_shouldMapCorrectly() {
        // Arrange
        Order order = new Order();
        order.setId(100L);
        order.setCustomerId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(200.00));

        OrderItem item = new OrderItem();
        item.setProductId(50L);
        item.setQuantity(2);
        item.setPriceAtPurchase(BigDecimal.valueOf(100.00));
        order.addOrderItem(item);

        // Act
        OrderResponse response = mapper.toResponse(order);

        // Assert
        assertEquals(100L, response.getOrderId());
        assertEquals(OrderStatus.PENDING, response.getOrderStatus());
        assertEquals(BigDecimal.valueOf(200.00), response.getTotalAmount());
        assertEquals(1, response.getItems().size());
        assertEquals(50L, response.getItems().get(0).getProductId());
    }

    @Test
    @DisplayName("Null Safety - Should return null when input is null")
    void handleNulls() {
        assertNull(mapper.toEntity(null));
        assertNull(mapper.toResponse(null));
    }
}