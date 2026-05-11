package com.itc.funkart.aggregator.controller;

import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.common.enums.order.OrderStatus;
import com.itc.funkart.aggregator.config.JwtService;
import com.itc.funkart.aggregator.config.JwtWebFilter;
import com.itc.funkart.aggregator.dto.OrderResponse;
import com.itc.funkart.aggregator.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class OrderControllerTest {

    private final Long MOCK_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private OrderService service;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private JwtWebFilter jwtWebFilter;

    @BeforeEach
    void setUp() {
        JwtUserDto mockUser = JwtUserDto.builder()
                .id(MOCK_USER_ID).name("Test User")
                .email("test@funkart.com").role("ROLE_USER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockUser, null, null));
    }

    @Test
    @DisplayName("GET /orders/{id} - returns order for authenticated user")
    void getOrder_success() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .orderId(101L).orderStatus(OrderStatus.PENDING).build();
        when(service.getOrderByIdAndUserId(101L, MOCK_USER_ID)).thenReturn(response);

        mockMvc.perform(get("/orders/{id}", 101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(101))
                .andExpect(jsonPath("$.message").value("Order retrieved successfully"));

        verify(service).getOrderByIdAndUserId(101L, MOCK_USER_ID);
    }

    @Test
    @DisplayName("PATCH /orders/{id}/cancel - delegates cancellation to service")
    void cancelOrder_success() throws Exception {
        mockMvc.perform(patch("/orders/{id}/cancel", 101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order cancellation successful"));

        verify(service).cancelOrder(101L, MOCK_USER_ID);
    }

    @Test
    @DisplayName("GET /orders/history - returns full history list")
    void getOrderHistory_success() throws Exception {
        List<OrderResponse> history = List.of(
                OrderResponse.builder().orderId(10L).orderStatus(OrderStatus.DELIVERED).build(),
                OrderResponse.builder().orderId(11L).orderStatus(OrderStatus.PENDING).build()
        );
        when(service.getCustomerOrderHistory(MOCK_USER_ID)).thenReturn(history);

        mockMvc.perform(get("/orders/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order history fetched"))
                .andExpect(jsonPath("$.data[0].orderId").value(10))
                .andExpect(jsonPath("$.data[1].orderId").value(11));

        verify(service).getCustomerOrderHistory(MOCK_USER_ID);
    }

    @Test
    @DisplayName("GET /orders/history - returns empty array when no orders")
    void getOrderHistory_empty() throws Exception {
        when(service.getCustomerOrderHistory(MOCK_USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/orders/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order history fetched"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(service).getCustomerOrderHistory(MOCK_USER_ID);
    }
}
