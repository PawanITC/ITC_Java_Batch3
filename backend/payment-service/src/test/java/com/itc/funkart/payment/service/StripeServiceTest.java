//package com.itc.funkart.payment.service;
//
//import com.stripe.exception.ApiConnectionException;
//import com.stripe.exception.StripeException;
//import com.stripe.model.PaymentIntent;
//import com.stripe.model.Refund;
//import com.stripe.net.RequestOptions;
//import com.stripe.param.PaymentIntentConfirmParams;
//import com.stripe.param.PaymentIntentCreateParams;
//import com.stripe.param.RefundCreateParams;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.MockedStatic;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.Mockito.*;
//
///**
// * <h2>StripeServiceTest</h2>
// * Validates low-level Stripe SDK communication, verifying metadata tagging,
// * correct idempotency key generation, and static method interception.
// */
//@ExtendWith(MockitoExtension.class)
//class StripeServiceTest {
//
//    private StripeService stripeService;
//    private MockedStatic<PaymentIntent> mockPaymentIntent;
//    private MockedStatic<Refund> mockRefund;
//
//    @BeforeEach
//    void setUp() {
//        stripeService = new StripeService();
//        // Registering static mocks in the ThreadLocal storage of the current JVM thread
//        mockPaymentIntent = mockStatic(PaymentIntent.class);
//        mockRefund = mockStatic(Refund.class);
//    }
//
//    @AfterEach
//    void tearDown() {
//        // Essential: Deregister to prevent "already registered" errors in subsequent tests
//        mockPaymentIntent.close();
//        mockRefund.close();
//    }
//
//    @Nested
//    @DisplayName("PaymentIntent Creation Logic")
//    class CreationTests {
//
//        @Test
//        @DisplayName("Create Intent: Verify Idempotency Prefix and Metadata")
//        void create_success() throws StripeException {
//            Long amount = 5000L;
//            String currency = "USD";
//            Long userId = 42L;
//            Long internalPaymentId = 101L;
//
//            PaymentIntent mockIntent = mock(PaymentIntent.class);
//            mockPaymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
//                    .thenReturn(mockIntent);
//
//            ArgumentCaptor<PaymentIntentCreateParams> paramsCaptor = ArgumentCaptor.forClass(PaymentIntentCreateParams.class);
//            ArgumentCaptor<RequestOptions> optionsCaptor = ArgumentCaptor.forClass(RequestOptions.class);
//
//            stripeService.createPaymentIntent(amount, currency, userId, internalPaymentId);
//
//            mockPaymentIntent.verify(() -> PaymentIntent.create(paramsCaptor.capture(), optionsCaptor.capture()));
//            assertEquals("create-pi-101", optionsCaptor.getValue().getIdempotencyKey());
//
//            PaymentIntentCreateParams params = paramsCaptor.getValue();
//            assertEquals("42", params.getMetadata().get("userId"));
//            assertEquals("101", params.getMetadata().get("paymentId"));
//        }
//
//        @Test
//        @DisplayName("Resilience: Propagate Stripe Exception on Network Failure")
//        void create_ApiError() {
//            // FIX: Use the existing class-level mock. DO NOT open a new one.
//            mockPaymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
//                    .thenThrow(new ApiConnectionException("Stripe API unreachable", null));
//
//            assertThrows(StripeException.class, () ->
//                    stripeService.createPaymentIntent(1000L, "usd", 1L, 1L));
//
//            mockPaymentIntent.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)));
//        }
//    }
//
//    @Nested
//    @DisplayName("Transaction Operations")
//    class OperationTests {
//
//        @Test
//        @DisplayName("Confirm: Should retrieve resource and confirm with idempotency key")
//        void confirm_success() throws StripeException {
//            String piId = "pi_123";
//            String pmId = "pm_card";
//
//            // Use the class-level mockPaymentIntent instead of opening a new one
//            PaymentIntent mockIntent = mock(PaymentIntent.class);
//
//            // 1. Mock the static retrieval
//            mockPaymentIntent.when(() -> PaymentIntent.retrieve(piId)).thenReturn(mockIntent);
//
//            // 2. Mock the instance-level confirmation
//            // Note: Since this is a standard mock object, use 'when', not 'mockPaymentIntent.when'
//            when(mockIntent.confirm(any(PaymentIntentConfirmParams.class), any(RequestOptions.class)))
//                    .thenReturn(mockIntent);
//
//            // 3. Execute
//            stripeService.confirmPaymentIntent(piId, pmId, "http://return.url");
//
//            // 4. Verifications
//            mockPaymentIntent.verify(() -> PaymentIntent.retrieve(piId));
//
//            // Verify instance-level confirm was called with the correct idempotency key
//            verify(mockIntent).confirm(any(PaymentIntentConfirmParams.class), argThat(options ->
//                    options != null && ("confirm-pi-" + piId).equals(options.getIdempotencyKey())
//            ));
//        }
//
//        @Test
//        @DisplayName("Refund: Verify Idempotency and Intent Mapping")
//        void refund_Success() throws StripeException {
//            String piId = "pi_refund_99";
//            Refund mockRefundObj = mock(Refund.class);
//
//            mockRefund.when(() -> Refund.create(any(RefundCreateParams.class), any(RequestOptions.class)))
//                    .thenReturn(mockRefundObj);
//
//            ArgumentCaptor<RequestOptions> optionsCaptor = ArgumentCaptor.forClass(RequestOptions.class);
//
//            stripeService.refundPayment(piId);
//
//            mockRefund.verify(() -> Refund.create(any(RefundCreateParams.class), optionsCaptor.capture()));
//            assertEquals("refund-pi_refund_99", optionsCaptor.getValue().getIdempotencyKey());
//        }
//
//        @Test
//        @DisplayName("Cancel: Execute cancellation on retrieved intent")
//        void cancel_Success() throws StripeException {
//            String piId = "pi_cancel_00";
//            PaymentIntent mockIntent = mock(PaymentIntent.class);
//
//            mockPaymentIntent.when(() -> PaymentIntent.retrieve(piId)).thenReturn(mockIntent);
//            when(mockIntent.cancel()).thenReturn(mockIntent);
//
//            stripeService.cancelPaymentIntent(piId);
//
//            verify(mockIntent).cancel();
//        }
//    }
//}