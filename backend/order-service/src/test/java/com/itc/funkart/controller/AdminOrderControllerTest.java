package com.itc.funkart.controller;

import com.itc.funkart.common.enums.order.OrderStatus;
import com.itc.funkart.config.JwtService;
import com.itc.funkart.config.JwtWebFilter;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>AdminOrderControllerTest</h2>
 * <p>
 * Verifies the administrative endpoints for order management.
 * This test ensures that the REST layer correctly delegates to the {@link OrderService}
 * and packages the results into a standardized API response.
 * </p>
 *
 * <h3>Technical Design:</h3>
 * <ul>
 *     <li><b>Heap Efficiency:</b> Uses Mockito to stub service calls, preventing the need
 *     for a real database connection or Hibernate session in the test <b>Heap</b>.</li>
 *     <li><b>Serialization Check:</b> Validates that Enums (like {@link OrderStatus})
 *     are correctly serialized to Strings in the JSON output.</li>
 *     <li><b>Security Context:</b> Operates with {@code addFilters = false} to isolate
 *     controller logic from the full Spring Security filter chain.</li>
 * </ul>
 */
@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
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
     * <h3>Test: Paginated Order Retrieval</h3>
     * <b>Endpoint:</b> {@code GET /api/v1/admin/orders}
     * <p>
     * Verifies that the admin can retrieve a paginated list of all orders.
     * This ensures the <b>JVM</b> efficiently handles data transfer via Spring Data {@link Page}.
     * </p>
     *
     * @throws Exception if the request execution fails.
     */
    @Test
    @DisplayName("GET /api/v1/admin/orders - Success")
    @WithMockUser(roles = "ADMIN")
    void getAllOrders_shouldReturnPaginatedResponse() throws Exception {
        // Arrange
        OrderResponse orderRes = OrderResponse.builder()
                .orderId(1L)
                .build();
        Page<OrderResponse> page = new PageImpl<>(List.of(orderRes));

        when(service.getAllOrders(any())).thenReturn(page);

        // Act & Assert (Fixed path to include /api/v1/admin)
        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].orderId").value(1))
                .andExpect(jsonPath("$.message").value("Admin order list retrieved"));

        verify(service).getAllOrders(any());
    }

    /**
     * <h3>Test: Administrative Status Override</h3>
     * <b>Endpoint:</b> {@code PATCH /api/v1/admin/orders/{id}/status}
     * <p>
     * Tests the ability of an admin to manually transition an order state (e.g., to SHIPPED).
     * Validates that the service receives the correct Enum type from the request parameters.
     * </p>
     *
     * @throws Exception if the request execution fails.
     */
    @Test
    @DisplayName("PATCH /api/v1/admin/orders/{id}/status - Success")
    @WithMockUser(roles = "ADMIN")
    void updateStatus_shouldReturnUpdatedOrder() throws Exception {
        // Arrange
        Long orderId = 1L;
        OrderResponse updatedRes = OrderResponse.builder()
                .orderId(orderId)
                .orderStatus(OrderStatus.SHIPPED)
                .build();

        when(service.updateOrderStatus(eq(orderId), eq(OrderStatus.SHIPPED)))
                .thenReturn(updatedRes);

        // Act & Assert (Fixed path to include /api/v1/admin)
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .param("newStatus", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.message").value("Order status updated to SHIPPED"));

        verify(service).updateOrderStatus(orderId, OrderStatus.SHIPPED);
    }
}