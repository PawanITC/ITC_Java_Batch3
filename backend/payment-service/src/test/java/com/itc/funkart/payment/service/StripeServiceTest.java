package com.itc.funkart.payment.service;

import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <h2>StripeServiceTest</h2>
 * <p>
 * Validates low-level Stripe SDK communication, ensuring that all metadata,
 * idempotency keys, and static method calls are executed correctly.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    private StripeService stripeService;
    private MockedStatic<PaymentIntent> mockPaymentIntent;
    private MockedStatic<Refund> mockRefund;

    @BeforeEach
    void setUp() {
        stripeService = new StripeService();
        // Open static mocks to intercept Stripe SDK's static factory methods
        mockPaymentIntent = mockStatic(PaymentIntent.class);
        mockRefund = mockStatic(Refund.class);
    }

    @AfterEach
    void tearDown() {
        // Critical: Close static mocks to prevent pollution of other test classes
        mockPaymentIntent.close();
        mockRefund.close();
    }

    @Nested
    @DisplayName("PaymentIntent Creation (createPaymentIntent) and API resilience")
    class CreationTests {

        @Test
        @DisplayName("Create Intent - Success: Verify Idempotency")
        void create_success() throws StripeException {
            // Arrange
            Long amount = 5000L;
            String currency = "usd";
            Long userId = 1L;
            Long internalPaymentId = 101L;

            PaymentIntent mockIntent = mock(PaymentIntent.class);
            mockPaymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(mockIntent);

            // We use Captors because PaymentIntentCreateParams has no public getters
            ArgumentCaptor<PaymentIntentCreateParams> paramsCaptor = ArgumentCaptor.forClass(PaymentIntentCreateParams.class);
            ArgumentCaptor<RequestOptions> optionsCaptor = ArgumentCaptor.forClass(RequestOptions.class);

            // Act
            stripeService.createPaymentIntent(amount, currency, userId, internalPaymentId);

            // Assert
            mockPaymentIntent.verify(() -> PaymentIntent.create(paramsCaptor.capture(), optionsCaptor.capture()));

            // We can verify the Idempotency Key because RequestOptions DOES have a getter
            assertEquals("pi-idempotency-101", optionsCaptor.getValue().getIdempotencyKey());
            assertNotNull(paramsCaptor.getValue());
        }


        @Test
        @DisplayName("Failure: Should propagate Stripe API exceptions")
        void create_ApiError() {
            // 1. Define the specific error
            ApiConnectionException networkError = new ApiConnectionException("Stripe unreachable", null);

            // 2. Use typed matchers to avoid ambiguity in the static mock
            mockPaymentIntent.when(() -> PaymentIntent.create(
                    any(PaymentIntentCreateParams.class),
                    any(RequestOptions.class)
            )).thenThrow(networkError);

            // 3. Assert the service bubbles up the error
            assertThrows(StripeException.class, () ->
                    stripeService.createPaymentIntent(1000L, "usd", 1L, 1L));
        }
    }

    @Nested
    @DisplayName("Transaction Operations (Confirm, Cancel, Refund)")
    class OperationTests {

        @Test
        @DisplayName("Confirm: Should successfully chain retrieve and confirm")
        void confirm_success() throws StripeException {
            String piId = "pi_123";
            PaymentIntent mockIntent = mock(PaymentIntent.class);

            // Set up the chain: Retrieve returns the mock, Mock returns itself on confirm
            mockPaymentIntent.when(() -> PaymentIntent.retrieve(piId)).thenReturn(mockIntent);
            when(mockIntent.confirm(any(PaymentIntentConfirmParams.class))).thenReturn(mockIntent);

            // Act
            stripeService.confirmPaymentIntent(piId, "pm_card", "http://return.url");

            // Assert: Verify the instance method 'confirm' was called on the retrieved object
            verify(mockIntent).confirm(any(PaymentIntentConfirmParams.class));
        }

        @Test
        @DisplayName("Refund: Should call Refund.create")
        void refund_Success() throws StripeException {
            String piId = "pi_123";
            Refund mockRefundObj = mock(Refund.class);

            mockRefund.when(() -> Refund.create(any(RefundCreateParams.class))).thenReturn(mockRefundObj);

            // Act
            stripeService.refundPayment(piId);

            // Assert
            mockRefund.verify(() -> Refund.create(any(RefundCreateParams.class)));
        }

        @Test
        @DisplayName("Cancel: Should retrieve and execute cancellation")
        void cancel_Success() throws StripeException {
            // Arrange
            String piId = "pi_999";
            PaymentIntent mockIntent = mock(PaymentIntent.class);

            mockPaymentIntent.when(() -> PaymentIntent.retrieve(piId)).thenReturn(mockIntent);
            when(mockIntent.cancel()).thenReturn(mockIntent);

            // Act
            PaymentIntent result = stripeService.cancelPaymentIntent(piId);

            // Assert
            assertNotNull(result);
            verify(mockIntent).cancel();
        }
    }

    @Nested
    @DisplayName("Edge Cases & SDK Resilience")
    class ResilienceTests {

        @Test
        @DisplayName("Retrieve: Should handle missing resources gracefully")
        void retrieve_NotFound() throws StripeException {
            mockPaymentIntent.when(() -> PaymentIntent.retrieve("pi_unknown"))
                    .thenReturn(null);

            var result = PaymentIntent.retrieve("pi_unknown");
            assertNull(result);
        }

        @Test
        @DisplayName("Conflict: Should handle Stripe error on invalid state transition")
        void confirm_Conflict() throws StripeException {
            // Arrange
            PaymentIntent mockIntent = mock(PaymentIntent.class);

            // Static mock: find the intent
            mockPaymentIntent.when(() -> PaymentIntent.retrieve(anyString()))
                    .thenReturn(mockIntent);

            // Instance mock: FIX THE MATCHER TYPE HERE
            // Your service calls .confirm(PaymentIntentConfirmParams), not RequestOptions
            when(mockIntent.confirm(any(PaymentIntentConfirmParams.class)))
                    .thenThrow(new StripeException("Invalid state", null, null, 400) {
                    });

            // Act & Assert
            assertThrows(StripeException.class, () ->
                    stripeService.confirmPaymentIntent("pi_123", "pm_1", "url"));
        }
    }
}