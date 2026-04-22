package com.itc.funkart.mapper;

import com.itc.funkart.dto.OrderRequest;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.Order;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderMapperTest {

    private final OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

    @Test
    void toEntity_shouldParseUuidStrings() {
        String customerId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();
        OrderRequest request = new OrderRequest(customerId, productId, 4, 44.0);

        Order entity = mapper.toEntity(request);

        assertEquals(UUID.fromString(customerId), entity.getCustomerId());
        assertEquals(UUID.fromString(productId), entity.getProductId());
        assertEquals(4, entity.getQuantity());
        assertEquals(44.0, entity.getPrice());
    }

    @Test
    void toEntity_shouldHandleNullRequest() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toResponse_shouldCopyAllValues() {
        Order order = Order.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(5)
                .price(50.0)
                .orderStatus("CREATED")
                .build();

        OrderResponse response = mapper.toResponse(order);

        assertEquals(order.getOrderId(), response.getOrderId());
        assertEquals(order.getCustomerId(), response.getCustomerId());
        assertEquals(order.getProductId(), response.getProductId());
        assertEquals(order.getQuantity(), response.getQuantity());
        assertEquals(order.getPrice(), response.getPrice());
        assertEquals(order.getOrderStatus(), response.getOrderStatus());
    }

    @Test
    void uuidToString_shouldHandleNull() {
        assertNull(mapper.uuidToString(null));
    }
}
