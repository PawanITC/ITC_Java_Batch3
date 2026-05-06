package com.itc.funkart.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.dto.request.CreatePaymentIntentRequest;
import com.itc.funkart.payment.dto.response.PaymentIntentResponse;
import com.itc.funkart.payment.dto.response.PaymentResponse;
import com.itc.funkart.payment.service.JwtService;
import com.itc.funkart.payment.service.PaymentService;
import com.itc.funkart.payment.service.StripeService;
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
import org.springframework.test.context.ActiveProfiles;
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
 * <h2>PaymentController Integration Test</h2>
 * <p>
 * Validates the REST contract and the "Unified Response Envelope" (ApiResponse).
 * Since the Controller is responsible for wrapping Service DTOs into ApiResponses,
 * these tests verify that the wrapping occurs correctly and timestamps are generated.
 * </p>
 */
@WebMvcTest(PaymentController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private StripeService stripeService;

    @MockitoBean
    private JwtService jwtService;

    private JwtUserDto mockUser;

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
     * Injects the mock user into the Security Context to satisfy @AuthenticationPrincipal.
     */
    private RequestPostProcessor authenticatedUser() {
        return request -> {
            var auth = new UsernamePasswordAuthenticationToken(mockUser, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
            return request;
        };
    }

    @Nested
    @DisplayName("Payment Initiation")
    class InitiationTests {

        @Test
        @DisplayName("POST /create-intent - Should wrap DTO in success envelope")
        void createIntent_Success() throws Exception {
            // GIVEN
            CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(101L, 5000L, "USD");
            PaymentIntentResponse serviceDto = new PaymentIntentResponse("sec_123", "pi_123", "requires_payment_method");

            // MOCK: Service returns the raw DTO as per your controller logic
            when(paymentService.createPaymentIntent(any(), any())).thenReturn(serviceDto);

            // WHEN & THEN
            mockMvc.perform(post("/payments/create-intent")
                            .with(authenticatedUser())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.paymentIntentId").value("pi_123"))
                    .andExpect(jsonPath("$.message").value("Payment intent created successfully"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.error").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Payment Confirmation & Reversal")
    class ActionTests {

        @Test
        @DisplayName("POST /confirm - Should finalize payment status")
        void confirmPayment_Success() throws Exception {
            ConfirmPaymentRequest request = new ConfirmPaymentRequest("pi_123", "pm_123", "http://url.com");
            PaymentResponse serviceDto = new PaymentResponse(1L, 1L, 101L, 5000L, "USD", "SUCCEEDED", LocalDateTime.now());

            when(paymentService.confirmPayment(any(), any())).thenReturn(serviceDto);

            mockMvc.perform(post("/payments/confirm")
                            .with(authenticatedUser())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                    .andExpect(jsonPath("$.message").value("Payment confirmed and processed"));
        }

        @Test
        @DisplayName("POST /{id}/refund - Should initiate refund status")
        void refundPayment_Success() throws Exception {
            PaymentResponse serviceDto = new PaymentResponse(1L, 1L, 101L, 5000L, "USD", "REFUNDED", LocalDateTime.now());

            when(paymentService.refundPayment(any(), anyLong())).thenReturn(serviceDto);

            mockMvc.perform(post("/payments/1/refund")
                            .with(authenticatedUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                    .andExpect(jsonPath("$.message").value("Refund successfully initiated"));
        }
    }

    @Nested
    @DisplayName("Retrieval Operations")
    class LookupTests {

        @Test
        @DisplayName("GET /{id} - Should return payment details")
        void getPayment_Success() throws Exception {
            PaymentResponse serviceDto = new PaymentResponse(1L, 1L, 101L, 5000L, "USD", "PENDING", LocalDateTime.now());

            when(paymentService.getPayment(any(), anyLong())).thenReturn(serviceDto);

            mockMvc.perform(get("/payments/1")
                            .with(authenticatedUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.orderId").value(101L))
                    .andExpect(jsonPath("$.message").value("Payment details retrieved"));
        }
    }
}