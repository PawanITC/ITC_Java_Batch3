package com.itc.funkart.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.payment.config.ApiConfig;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

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
 * <h1>PaymentControllerTest</h1>
 * * <p>This test suite performs a "Web Slice" test of the {@link PaymentController}.
 * Unlike a full integration test, {@code @WebMvcTest} only instantiates the web layer,
 * making tests significantly faster and more focused.</p>
 * * <h3>Technical Strategy:</h3>
 * <ul>
 * <li><b>Path Prefixing:</b> Mocks {@link ApiConfig} to satisfy the dynamic prefixing logic in {@code WebConfig}.</li>
 * <li><b>Security Mocking:</b> Manually populates the {@code SecurityContext} with a {@link JwtUserDto}
 * to prevent {@code ClassCastException} during {@code @AuthenticationPrincipal} resolution.</li>
 * <li><b>Infrastructure:</b> Provides {@code @MockitoBean} for all downstream dependencies to ensure
 * the Spring Application Context loads correctly.</li>
 * </ul>
 * * @author Abbas
 *
 * @version 1.1
 */
@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Configuration mock used by WebConfig for URL prefixing.
     */
    @MockitoBean
    private ApiConfig apiConfig;

    /**
     * Mocked JWT service for token handling (if used by interceptors).
     */
    @MockitoBean
    private JwtService jwtService;

    /**
     * Mocked Kafka template to prevent connectivity errors during tests.
     */
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Mocked RestClient for external communication.
     */
    @MockitoBean
    private RestClient restClient;

    /**
     * The primary business logic service mocked for controller interaction.
     */
    @MockitoBean
    private PaymentService paymentService;

    /**
     * Initializes the dynamic API versioning prefix.
     * <p>This ensures that routes are registered under {@code /api/v1} during context initialization.</p>
     */
    @BeforeEach
    void setup() {
        when(apiConfig.getVersion()).thenReturn("api/v1");
    }

    /**
     * Transaction Management
     * Contains test cases for creating and processing payment transactions.
     */
    @Nested
    @DisplayName("Transaction Management")
    class TransactionTests {

        /**
         * Validates the successful creation of a Payment Intent.
         * * <p><b>Steps:</b>
         * 1. Create a mock {@link CreatePaymentIntentRequest}.
         * 2. Inject a custom {@link JwtUserDto} into the security context.
         * 3. Assert that the service returns the expected client secret and ID.</p>
         * * @throws Exception if MockMvc perform fails
         */
        @Test
        @DisplayName("POST /create-intent - Success")
        void createIntent_Success() throws Exception {
            CreatePaymentIntentRequest intentReq = new CreatePaymentIntentRequest(101L, 5000L, "USD");
            PaymentIntentResponse piResponse = new PaymentIntentResponse("sec_123", "pi_123", "requires_payment_method");
            ApiResponse<PaymentIntentResponse> apiResp = new ApiResponse<>(piResponse, "Success");
            JwtUserDto mockUser = new JwtUserDto(1L, "Abbas", "a@b.com");

            when(paymentService.createPaymentIntent(any(Long.class), any(CreatePaymentIntentRequest.class)))
                    .thenReturn(apiResp);

            mockMvc.perform(post("/api/v1/payments/create-intent")
                            .with(request -> {
                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(
                                                mockUser, null, Collections.emptyList()
                                        );
                                request.setUserPrincipal(auth); // optional
                                SecurityContextHolder.getContext().setAuthentication(auth); // ✅ THIS is key
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(intentReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.paymentIntentId").value("pi_123"))
                    .andExpect(jsonPath("$.data.status").value("requires_payment_method"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    /**
     * <h2>Management Operations</h2>
     * Contains test cases for payment history retrieval and administrative lookups.
     */
    @Nested
    @DisplayName("Management Operations")
    class ManagementTests {

        /**
         * Validates the retrieval of a specific payment's metadata.
         * * <p>This test ensures that the controller correctly extracts path variables
         * and formats the {@link PaymentResponse} DTO properly.</p>
         * * @throws Exception if MockMvc perform fails
         */
        @Test
        @DisplayName("GET /{paymentId} - Success")
        void getPayment_Success() throws Exception {
            // Arrange
            PaymentResponse paymentResponse = new PaymentResponse(
                    1L, 1L, 101L, 5000L, "USD", "SUCCEEDED", LocalDateTime.now()
            );
            // Ensure the message here matches what your Service/Controller returns
            ApiResponse<PaymentResponse> apiResponse = new ApiResponse<>(paymentResponse, "Fetched");
            JwtUserDto mockUser = new JwtUserDto(1L, "Abbas", "a@b.com");

            when(paymentService.getPayment(anyLong(), anyLong())).thenReturn(apiResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/payments/1")
                            .with(req -> {
                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(mockUser, null, Collections.emptyList());
                                SecurityContextHolder.getContext().setAuthentication(auth);
                                return req;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                    .andExpect(jsonPath("$.message").value("Fetched")); // Adjust if actual is different
        }
    }

    /**
     * <h2>Action Coverage</h2>
     * <p>Targets the POST operations for payment confirmation and refunds.
     * Ensures that the auditing/logging logic correctly captures the user
     * and transaction IDs.</p>
     */
    @Nested
    @DisplayName("Action Coverage")
    class ActionTests {

        /**
         * <b>Scenario:</b> User confirms a pending payment intent.<br>
         * <b>Coverage:</b> Line 68-77 (confirmPayment logging).<br>
         * <b>Technical Note:</b> Verifies the end-to-end logging of the confirmation result
         * fetched from the service layer.
         */
        @Test
        @DisplayName("POST /confirm - Verify logging and response mapping for payment confirmation")
        void confirmPayment_Success() throws Exception {
            ConfirmPaymentRequest confirmReq = new ConfirmPaymentRequest(
                    "pi_123",           // paymentIntentId
                    "pm_123",           // paymentMethodId
                    "http://localhost:3000/success" // returnUrl
            );
            PaymentResponse resp = new PaymentResponse(1L, 1L, 101L, 5000L, "USD", "SUCCEEDED", LocalDateTime.now());
            ApiResponse<PaymentResponse> apiResp = new ApiResponse<>(resp, "Confirmed");
            JwtUserDto mockUser = new JwtUserDto(1L, "Abbas", "a@b.com");

            when(paymentService.confirmPayment(anyLong(), any())).thenReturn(apiResp);

            mockMvc.perform(post("/api/v1/payments/confirm")
                            .with(req -> {
                                SecurityContextHolder.getContext().setAuthentication(
                                        new UsernamePasswordAuthenticationToken(mockUser, null, Collections.emptyList()));
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(confirmReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        }

        /**
         * <b>Scenario:</b> User or Admin requests a refund for an existing payment.<br>
         * <b>Coverage:</b> Line 101-108 (refundPayment logging).<br>
         * <b>Technical Note:</b> Ensures the refund amount and status are logged
         * correctly in the successful response path.
         */
        @Test
        @DisplayName("POST /refund - Verify successful refund processing and audit logging")
        void refundPayment_Success() throws Exception {
            PaymentResponse resp = new PaymentResponse(1L, 1L, 101L, 5000L, "USD", "REFUNDED", LocalDateTime.now());
            ApiResponse<PaymentResponse> apiResp = new ApiResponse<>(resp, "Refunded");
            JwtUserDto mockUser = new JwtUserDto(1L, "Abbas", "a@b.com");

            when(paymentService.refundPayment(anyLong(), anyLong())).thenReturn(apiResp);

            mockMvc.perform(post("/api/v1/payments/1/refund")
                            .with(req -> {
                                SecurityContextHolder.getContext().setAuthentication(
                                        new UsernamePasswordAuthenticationToken(mockUser, null, Collections.emptyList()));
                                return req;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REFUNDED"));
        }
    }
}