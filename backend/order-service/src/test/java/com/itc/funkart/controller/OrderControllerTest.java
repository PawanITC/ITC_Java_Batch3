package com.itc.funkart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.dto.OrderRequest;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService service;

    @Test
    void createOrder_shouldReturnCreatedPayload() throws Exception {
        OrderRequest request = new OrderRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 2, 19.99);
        OrderResponse response = OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.fromString(request.getCustomerId()))
                .productId(UUID.fromString(request.getProductId()))
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .orderStatus("CREATED")
                .build();

        when(service.createOrder(any(OrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        verify(service, times(1)).createOrder(any(OrderRequest.class));
    }

    @Test
    void getOrder_shouldReturnResource() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = OrderResponse.builder()
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(1)
                .price(9.95)
                .orderStatus("CREATED")
                .build();

        when(service.getOrder(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        verify(service, times(1)).getOrder(orderId);
    }

    @Test
    void getOrders_shouldReturnList() throws Exception {
        OrderResponse first = OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(1)
                .price(5.0)
                .orderStatus("CREATED")
                .build();
        OrderResponse second = OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(2)
                .price(12.0)
                .orderStatus("CREATED")
                .build();

        when(service.getAllOrders()).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(first, second))));

        verify(service, times(1)).getAllOrders();
    }

    @Test
    void updateOrder_shouldReturnUpdatedPayload() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderRequest request = new OrderRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 3, 27.5);
        OrderResponse response = OrderResponse.builder()
                .orderId(orderId)
                .customerId(UUID.fromString(request.getCustomerId()))
                .productId(UUID.fromString(request.getProductId()))
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .orderStatus("UPDATED")
                .build();

        when(service.updateOrder(orderId, request)).thenReturn(response);

        mockMvc.perform(put("/api/v1/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        verify(service, times(1)).updateOrder(orderId, request);
    }

    @Test
    void deleteOrder_shouldReturnConfirmation() throws Exception {
        UUID orderId = UUID.randomUUID();

        doNothing().when(service).deleteOrder(orderId);

        mockMvc.perform(delete("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(content().string("Order deleted"));

        verify(service, times(1)).deleteOrder(orderId);
    }
}
