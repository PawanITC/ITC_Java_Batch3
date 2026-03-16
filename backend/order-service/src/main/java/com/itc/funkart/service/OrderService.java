package com.itc.funkart.service;

import com.itc.funkart.dto.OrderRequest;
import com.itc.funkart.dto.OrderResponse;

import java.util.List;
import java.util.UUID;


    public interface OrderService {

        OrderResponse createOrder(OrderRequest request);

        OrderResponse getOrder(UUID id);

        List<OrderResponse> getAllOrders();

        OrderResponse updateOrder(UUID id, OrderRequest request);

        void deleteOrder(UUID id);
    }

