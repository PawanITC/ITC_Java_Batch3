package com.itc.funkart.controller;

import com.itc.funkart.config.JwtService;
import com.itc.funkart.config.JwtWebFilter;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.OrderStatus;
import com.itc.funkart.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>OrderControllerTest</h2>
 * <p>
 * Verifies the customer-facing {@link OrderController}.
 * Ensures that responses are correctly structured and service methods are invoked properly.
 * </p>
 */
@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtWebFilter jwtWebFilter;

    /**
     * <h3>Test: Get Order by ID</h3>
     * <b>Scenario:</b> A user requests details for a specific order they own.<br>
     * <b>Expected:</b> Returns a 200 OK with the order data wrapped in an ApiResponse.
     */
    @Test
    @DisplayName("GET /orders/{id} - Success")
    @WithMockUser
    void getOrder_shouldReturnOrderDetails() throws Exception {
        Long orderId = 101L;
        OrderResponse response = OrderResponse.builder()
                .orderId(orderId)
                .orderStatus(OrderStatus.PENDING)
                .build();

        when(service.getOrder(orderId)).thenReturn(response);

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.message").value("Order retrieved successfully"));
    }

    /**
     * <h3>Test: Order Cancellation</h3>
     * <b>Scenario:</b> A user cancels their order before it is shipped.<br>
     * <b>Expected:</b> The status is updated to CANCELLED and a success message is returned.
     */
    @Test
    @DisplayName("PATCH /orders/{id}/cancel - Success")
    @WithMockUser
    void cancelOrder_shouldInvokeCancellation() throws Exception {
        Long orderId = 101L;

        mockMvc.perform(patch("/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order cancellation request processed"));

        // Verify the controller specifically asked for a CANCELLED status update
        verify(service).updateOrderStatus(eq(orderId), eq(OrderStatus.CANCELLED));
    }
}