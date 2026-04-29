package com.itc.funkart.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.payment.dto.jwt.JwtUserDto;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.dto.request.CreatePaymentIntentRequest;
import com.itc.funkart.payment.dto.response.PaymentIntentResponse;
import com.itc.funkart.payment.dto.response.PaymentResponse;
import com.itc.funkart.payment.response.ApiResponse;
import com.itc.funkart.payment.service.JwtService;
import com.itc.funkart.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>PaymentControllerTest</h2>
 * <p>
 * This test suite performs a <b>Web Slice</b> test of the {@link PaymentController}.
 * It validates the REST contract, JSON serialization, and URI mapping without
 * initializing the full application service layer.
 * </p>
 *
 * <h3>Security Strategy:</h3>
 * <p>
 * To satisfy {@code @AuthenticationPrincipal} in the controller, we use a
 * {@link RequestPostProcessor} (the {@code authenticatedUser()} method).
 * This manually populates the {@link SecurityContextHolder} for each request,
 * ensuring that the principal is resolved correctly even when Spring Security
 * filters are disabled ({@code addFilters = false}).
 * </p>
 *
 * @author Abbas
 * @version 2.0
 */
@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtService jwtService;

    private JwtUserDto mockUser;

    /**
     * Pre-test setup to initialize common test data.
     */
    @BeforeEach
    void setup() {
        mockUser = JwtUserDto.builder()
                .id(1L)
                .name("Abbas")
                .email("a@b.com")
                .role("ROLE_USER")
                .build();
    }

    /**
     * A factory method for creating a security-aware request processor.
     * Injecting the {@link JwtUserDto} into the security context prevents
     * {@link NullPointerException} when the controller accesses the principal.
     *
     * @return A RequestPostProcessor containing the authenticated mock user.
     */
    private RequestPostProcessor authenticatedUser() {
        return request -> {
            var auth = new UsernamePasswordAuthenticationToken(
                    mockUser, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
            return request;
        };
    }

    /**
     * <h3>Transaction Lifecycle Tests</h3>
     * Validates endpoints that initiate or finalize payments via Stripe.
     */
    @Nested
    @DisplayName("Transaction Management")
    class TransactionTests {

        /**
         * Verifies that valid intent requests return a 200 OK and the client secret.
         * Matches against the dynamic JSON path {@code $.data.paymentIntentId}.
         */
        @Test
        @DisplayName("POST /create-intent - Success")
        void createIntent_Success() throws Exception {
            CreatePaymentIntentRequest intentReq = new CreatePaymentIntentRequest(101L, 5000L, "USD");
            PaymentIntentResponse piResponse = new PaymentIntentResponse("sec_123", "pi_123", "requires_payment_method");

            when(paymentService.createPaymentIntent(any(), any()))
                    .thenReturn(new ApiResponse<>(piResponse, "Success"));

            mockMvc.perform(post("/payments/create-intent")
                            .with(authenticatedUser())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(intentReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.paymentIntentId").value("pi_123"));
        }
    }

    /**
     * <h3>Lookup Operations</h3>
     * Tests for fetching existing payment data.
     */
    @Nested
    @DisplayName("Management Operations")
    class ManagementTests {

        /**
         * Validates the retrieval of a payment record by its internal ID.
         * Ensures correct mapping of status enums in the JSON response.
         */
        @Test
        @DisplayName("GET /{paymentId} - Success")
        void getPayment_Success() throws Exception {
            PaymentResponse paymentResponse = new PaymentResponse(
                    1L, 1L, 101L, 5000L, "USD", "SUCCEEDED", LocalDateTime.now()
            );

            when(paymentService.getPayment(any(), anyLong()))
                    .thenReturn(new ApiResponse<>(paymentResponse, "Fetched"));

            mockMvc.perform(get("/payments/1")
                            .with(authenticatedUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        }
    }

    /**
     * <h3>State Mutation Tests</h3>
     * Tests logic for confirming or reversing transaction funds.
     */
    @Nested
    @DisplayName("Action Coverage")
    class ActionTests {

        /**
         * Validates the confirmation flow for a pending Stripe PaymentIntent.
         */
        @Test
        @DisplayName("POST /confirm - Success")
        void confirmPayment_Success() throws Exception {
            ConfirmPaymentRequest confirmReq = new ConfirmPaymentRequest("pi_123", "pm_123", "http://return.url");
            PaymentResponse resp = new PaymentResponse(1L, 1L, 101L, 5000L, "USD", "SUCCEEDED", LocalDateTime.now());

            when(paymentService.confirmPayment(any(), any()))
                    .thenReturn(new ApiResponse<>(resp, "Confirmed"));

            mockMvc.perform(post("/payments/confirm")
                            .with(authenticatedUser())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(confirmReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        }

        /**
         * Verifies the refund logic, ensuring the response reflects the "REFUNDED" status.
         */
        @Test
        @DisplayName("POST /refund - Success")
        void refundPayment_Success() throws Exception {
            PaymentResponse resp = new PaymentResponse(1L, 1L, 101L, 5000L, "USD", "REFUNDED", LocalDateTime.now());

            when(paymentService.refundPayment(any(), anyLong()))
                    .thenReturn(new ApiResponse<>(resp, "Refunded"));

            mockMvc.perform(post("/payments/1/refund")
                            .with(authenticatedUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REFUNDED"));
        }
    }
}