package com.itc.funkart.payment.integration;

import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.payment.exception.PaymentException;
import com.itc.funkart.payment.repository.PaymentRepository;
import com.itc.funkart.payment.service.JwtService;
import com.itc.funkart.payment.service.StripeService;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class PaymentIntegrationTest {

    private static final String CREATE_INTENT_PATH = "/payments/create-intent";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private StripeService stripeService;

    private String validToken;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();

        JwtUserDto mockUser = JwtUserDto.builder()
                .id(1L)
                .name("Abbas")
                .email("abbas@example.com")
                .role("ROLE_USER")
                .build();

        validToken = jwtService.generateJwtToken(mockUser);
    }

    @Nested
    @DisplayName("Payment Intent Creation")
    class CreateIntentTests {

        @Test
        @DisplayName("Should persist payment and return PaymentIntentResponse")
        void shouldSucceedHappyPath() throws Exception {

            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_mock_789");
            when(mockIntent.getClientSecret()).thenReturn("secret_mock_789");

            when(stripeService.createPaymentIntent(
                    anyLong(), anyString(), anyLong(), anyLong()
            )).thenReturn(mockIntent);

            String requestBody = """
                    {
                        "amount": 1500,
                        "currency": "usd",
                        "orderId": 101
                    }
                    """;

            mockMvc.perform(post(CREATE_INTENT_PATH)
                            .header("Authorization", "Bearer " + validToken)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.paymentIntentId").value("pi_mock_789"))
                    .andExpect(jsonPath("$.data.clientSecret").value("secret_mock_789"));

            assertEquals(1, paymentRepository.count());

            verify(stripeService, times(1))
                    .createPaymentIntent(anyLong(), anyString(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("Should return 400 when PaymentException is thrown")
        void shouldReturn400OnPaymentException() throws Exception {

            String requestBody = """
                    {
                        "amount": 1000,
                        "currency": "usd",
                        "orderId": 202
                    }
                    """;

            when(stripeService.createPaymentIntent(
                    anyLong(), anyString(), anyLong(), anyLong()
            )).thenThrow(new PaymentException(
                    "PAYMENT_FAILED",
                    "Invalid payment details"
            ));

            mockMvc.perform(post(CREATE_INTENT_PATH)
                            .header("Authorization", "Bearer " + validToken)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("PAYMENT_PROCESSING_ERROR"))
                    .andExpect(jsonPath("$.error.message")
                            .value(containsString("PAYMENT_FAILED")));

            verify(stripeService, times(1))
                    .createPaymentIntent(anyLong(), anyString(), anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("Security & Webhook Access")
    class SecurityTests {

        @Test
        @DisplayName("Should reject create-intent without JWT")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post(CREATE_INTENT_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should allow public access to webhook endpoint")
        void webhookShouldBePublic() throws Exception {

            mockMvc.perform(post("/payments/webhook")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}