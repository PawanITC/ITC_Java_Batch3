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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>AdminOrderControllerTest</h2>
 *
 * <p>
 * Unit tests for {@link AdminOrderController}. These tests validate:
 * <ul>
 *     <li>Administrative access to order data</li>
 *     <li>Pagination handling</li>
 *     <li>Status updates for orders</li>
 * </ul>
 * </p>
 *
 * <p>
 * Security filters are disabled for isolation, but role-based annotations are still used
 * to document intended access control.
 * </p>
 */
@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtWebFilter jwtFilter;

    /**
     * <h3>GET /admin/orders - Paginated retrieval</h3>
     *
     * <p>
     * Scenario: Admin requests all orders with pagination.
     * Expected: A paginated response wrapped in ApiResponse.
     * </p>
     */
    @Test
    @DisplayName("GET /admin/orders - should return paginated orders")
    @WithMockUser(roles = "ADMIN")
    void getAllOrders_shouldReturnPaginatedResponse() throws Exception {

        OrderResponse orderRes = OrderResponse.builder()
                .orderId(1L)
                .build();

        Page<OrderResponse> page = new PageImpl<>(List.of(orderRes));

        when(service.getAllOrders(any())).thenReturn(page);

        mockMvc.perform(get("/admin/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].orderId").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.message").value("Admin order list retrieved"));

        verify(service, times(1)).getAllOrders(any());
    }

    /**
     * <h3>PATCH /admin/orders/{id}/status</h3>
     *
     * <p>
     * Scenario: Admin updates order status to SHIPPED.
     * Expected: Order is updated and returned in response.
     * </p>
     */
    @Test
    @DisplayName("PATCH /admin/orders/{id}/status - should update order status")
    @WithMockUser(roles = "ADMIN")
    void updateStatus_shouldReturnUpdatedOrder() throws Exception {

        Long orderId = 1L;

        OrderResponse updatedRes = OrderResponse.builder()
                .orderId(orderId)
                .orderStatus(OrderStatus.SHIPPED)
                .build();

        when(service.updateOrderStatus(eq(orderId), eq(OrderStatus.SHIPPED)))
                .thenReturn(updatedRes);

        mockMvc.perform(patch("/admin/orders/{id}/status", orderId)
                        .param("newStatus", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.message").value("Order status updated to SHIPPED"));

        verify(service).updateOrderStatus(orderId, OrderStatus.SHIPPED);
    }
}