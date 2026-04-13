package com.itc.funkart.mapper;

import com.itc.funkart.dto.OrderRequest;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    // Convert DTO → Entity (handle UUID manually)
    default Order toEntity(OrderRequest request) {
        if (request == null) return null;

        Order order = new Order();
        if (request.getCustomerId() != null)
            order.setCustomerId(UUID.fromString(request.getCustomerId()));
        if (request.getProductId() != null)
            order.setProductId(UUID.fromString(request.getProductId()));

        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());

        return order;
    }

    // Convert Entity → DTO
    @Mapping(source = "customerId", target = "customerId", qualifiedByName = "uuidToString")
    @Mapping(source = "productId", target = "productId", qualifiedByName = "uuidToString")
    @Mapping(source = "orderId", target = "orderId", qualifiedByName = "uuidToString")
    OrderResponse toResponse(Order order);

    // Helper methods to convert UUID → String (MapStruct will use this)
    @Named("uuidToString")
    default String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }
}