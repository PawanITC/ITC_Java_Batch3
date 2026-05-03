package com.itc.funkart.controller;

import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.common.enums.order.OrderStatus;
import com.itc.funkart.config.JwtService;
import com.itc.funkart.config.JwtWebFilter;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.service.OrderService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>OrderControllerTest</h2>
 * <p>
 * Performs unit testing on the {@link OrderController} layer using {@link MockMvc}.
 * This class ensures that REST endpoints correctly handle user authentication data
 * and delegate business logic to the service layer.
 * </p>
 *
 * <h3>Testing Strategy:</h3>
 * <ul>
 *     <li><b>Security Isolation:</b> Uses {@code addFilters = false} to bypass the full filter chain,
 *     manually injecting {@link JwtUserDto} into the {@link SecurityContextHolder}.</li>
 *     <li><b>Service Mocking:</b> Leverages {@link MockitoBean} to prevent actual DB/Kafka interactions,
 *     preserving <b>Heap</b> space during test execution.</li>
 * </ul>
 */
@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class OrderControllerTest {

    /**
     * Static User ID used for verifying ownership logic across all tests.
     */
    private final Long MOCK_USER_ID = 1L;
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private OrderService service;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private JwtWebFilter jwtWebFilter;

    /**
     * Initializes the Security Context before each test execution.
     * <p>
     * This method creates a mock {@link JwtUserDto} and places it in the
     * {@link SecurityContextHolder}. This prevents {@code NullPointerException}
     * when the controller's {@code @AuthenticationPrincipal} argument is resolved.
     * </p>
     */
    @BeforeEach
    void setUp() {
        JwtUserDto mockUser = JwtUserDto.builder()
                .id(MOCK_USER_ID)
                .name("Test User")
                .email("test@funkart.com")
                .role("ROLE_USER")
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(mockUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * <b>Test:</b> Fetch specific order details.
     * <p>
     * Verifies that:
     * <ol>
     *     <li>The request hits the correct URI: {@code /api/v1/orders/{id}}.</li>
     *     <li>The Service is called with both the Order ID and the extracted User ID.</li>
     *     <li>The response JSON structure follows the {@code ApiResponse} standard.</li>
     * </ol>
     * </p>
     *
     * @throws Exception if the MockMvc request fails.
     */
    @Test
    @DisplayName("GET /api/v1/orders/{id} - Success")
    void getOrder_shouldReturnOrderDetails() throws Exception {
        Long orderId = 101L;
        OrderResponse response = OrderResponse.builder()
                .orderId(orderId)
                .orderStatus(OrderStatus.PENDING)
                .build();

        when(service.getOrderByIdAndUserId(eq(orderId), eq(MOCK_USER_ID))).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.message").value("Order retrieved successfully"));

        verify(service).getOrderByIdAndUserId(orderId, MOCK_USER_ID);
    }

    /**
     * <b>Test:</b> Customer-initiated order cancellation.
     * <p>
     * Validates that the {@code PATCH} request successfully triggers the cancellation
     * flow in the service layer using the authenticated user's ID.
     * </p>
     *
     * @throws Exception if the MockMvc request fails.
     */
    @Test
    @DisplayName("PATCH /api/v1/orders/{id}/cancel - Success")
    void cancelOrder_shouldInvokeCancellation() throws Exception {
        Long orderId = 101L;

        mockMvc.perform(patch("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order cancellation successful"));

        verify(service).cancelOrder(eq(orderId), eq(MOCK_USER_ID));
    }
}