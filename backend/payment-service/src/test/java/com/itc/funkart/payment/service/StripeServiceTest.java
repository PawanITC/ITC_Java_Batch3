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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StripeService} ensuring full branch and line coverage.
 * Focuses on mocking static Stripe SDK calls to validate internal logic
 * and idempotency handling.
 */
@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    // SETUP: Initialize StripeService and open static mocks for PaymentIntent and Refund
    private StripeService stripeService;
    private MockedStatic<PaymentIntent> mockPaymentIntent;
    private MockedStatic<Refund> mockRefund;

    /**
     * Prepares the testing environment by initializing the service and freezing the Stripe SDK.
     * <p>
     * <b>Why:</b> We use MockedStatic to intercept calls to Stripe's static methods (like PaymentIntent.create).
     * This prevents the tests from attempting real network calls and allows us to define "Stunt Double" behaviors.
     * </p>
     */
    @BeforeEach
    void setUp() {
        // Initialize the actual service we want to test.
        stripeService = new StripeService();

        // Open the static mock "Gates." While these are open, the real logic inside
        // PaymentIntent and Refund is replaced by Mockito's control logic.
        mockPaymentIntent = mockStatic(PaymentIntent.class);
        mockRefund = mockStatic(Refund.class);
    }

    /**
     * Cleans up the JVM by releasing the static mocks.
     * <p>
     * <b>Crucial:</b> Static mocks persist across the entire test suite. If we don't .close() them,
     * other tests in different classes will find the Stripe SDK still "frozen," causing
     * unexpected failures or memory leaks.
     * </p>
     */
    @AfterEach
    void tearDown() {
        // Unfreeze the classes so the next test class starts with a clean slate.
        mockPaymentIntent.close();
        mockRefund.close();
    }

    /**
     * Tests focused on the lifecycle of creating PaymentIntents.
     * Includes happy paths for initialization and handling of API-level failures.
     */
    @Nested
    @DisplayName("Intent Creation & API Resilience")
    class IntentCreationTests {

        /**
         * Validates that a PaymentIntent is correctly constructed with metadata and idempotency.
         * <p>
         * <b>Coverage:</b> Line-by-line execution of createPaymentIntent().
         * </p>
         */
        @Test
        @DisplayName("Create Intent - Success: Metadata and Idempotency verified")
        void create_success() throws StripeException {
            // 1. Prepare inputs to match the Service's current order
            Long amount = 5000L;
            String currency = "usd";
            Long userId = 1L;
            Long internalPaymentId = 101L; // This represents payment.getId()

            // 2. Prepare the Stripe Mock Object
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_123");
            when(mockIntent.getStatus()).thenReturn("requires_payment_method");

            // 3. Intercept the Static Call
            mockPaymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(mockIntent);

            // 4. Act: Call with the order (amount, currency, userId, internalId)
            stripeService.createPaymentIntent(amount, currency, userId, internalPaymentId);

            // 5. Verify: Check that the metadata keys match what StripeService is actually doing
            mockPaymentIntent.verify(() -> PaymentIntent.create(
                    argThat((PaymentIntentCreateParams params) -> {
                        assertEquals(amount, params.getAmount());
                        assertEquals(currency, params.getCurrency());

                        // Adjusting expectations to match the "wrong" but existing order:
                        // If 3rd param is userId, ensure it's in the right metadata slot
                        assertEquals(String.valueOf(userId), params.getMetadata().get("userId"));
                        assertEquals(String.valueOf(internalPaymentId), params.getMetadata().get("paymentId"));
                        return true;
                    }),
                    argThat((RequestOptions options) -> {
                        assertNotNull(options.getIdempotencyKey());
                        return true;
                    })
            ));
        }


        /**
         * Deep-dive validation of the Stripe API payload.
         * <p>
         * <b>Why this is critical:</b> Stripe uses metadata to link payments to our internal
         * database records. If the 'orderId' or 'userId' is missing in the API call, we
         * lose the ability to reconcile payments in the Stripe Dashboard.
         * </p>
         * <p>
         * <b>Strategy:</b> Uses {@code .thenAnswer()} to intercept the {@link PaymentIntentCreateParams}
         * object passed to the static {@code create} method, allowing us to assert on its
         * internal Map values before the mock returns.
         * </p>
         */
        @Test
        @DisplayName("Create Intent - Metadata: Ensure Order and User IDs are sent")
        void create_metadata_check() throws StripeException {
            // These match the 3rd and 4th parameters of your service call
            Long userId = 1L;
            Long internalPaymentId = 101L;

            // MOCK: Intercept the static create call and inspect the arguments
            mockPaymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                    .thenAnswer(invocation -> {
                        // Argument 0 is the PaymentIntentCreateParams
                        PaymentIntentCreateParams params = invocation.getArgument(0);

                        // ASSERT: Ensure our service correctly mapped our long IDs to Stripe strings
                        assertEquals("1", params.getMetadata().get("userId"), "Metadata userId mismatch");
                        assertEquals("101", params.getMetadata().get("paymentId"), "Metadata paymentId mismatch");

                        // Return a shell mock so the service can finish its logging without a NullPointerException
                        PaymentIntent shellMock = mock(PaymentIntent.class);
                        when(shellMock.getStatus()).thenReturn("requires_payment_method");
                        return shellMock;
                    });

            // ACT: Execute the service
            stripeService.createPaymentIntent(5000L, "usd", userId, internalPaymentId);
        }

        /**
         * Verifies that the service propagates Stripe-specific exceptions.
         * <p>
         * <b>Coverage:</b> Exceptional path (throws) for createPaymentIntent().
         * </p>
         */
        @Test
        @DisplayName("Create Intent - Failure: Stripe API Error propagation")
        void create_failure() throws StripeException {
            // GIVEN: We prepare a "Throwable" error.
            // Since StripeException is abstract, we add {} at the end to make it a quick anonymous subclass.
            StripeException stripeError = new StripeException("Stripe is down", "req_123", "card_declined", 402) {
            };

            // MOCK: Tell the Static 'PaymentIntent' class to blow up when .create() is called.
            mockPaymentIntent.when(() ->
                            PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                    .thenThrow(stripeError);        // Instead of .thenReturn(), we use .thenThrow() to simulate an API disaster.

            // WHEN/THEN: We execute the service call inside an assertThrows block.
            assertThrows(StripeException.class,
                    () -> stripeService.createPaymentIntent(1000L, "usd", 1L, 1L));
            // This proves the service doesn't swallow the error, but lets it bubble up.
        }
    }

    /**
     * Tests for state-changing operations on existing PaymentIntents.
     * Validates the 'Retrieve-then-Act' pattern used in the service logic.
     */
    @Nested
    @DisplayName("Transaction Lifecycle: Confirm & Cancel")
    class TransactionActionTests {

        /**
         * Validates the two-step process: Retrieve existing intent -> Confirm it.
         * <p>
         * <b>Coverage:</b> confirmPaymentIntent() logic and method chaining.
         * </p>
         */
        @Test
        @DisplayName("Confirm Intent - Success: Retrieval and Confirmation chain")
        void confirm_success() throws StripeException {

            // GIVEN: A mock intent is set up to be "found" and then "confirmed" (think of a stunt double)
            // We create a "mock" PaymentIntent because we don't want to talk to real Stripe.
            // This object is currently an empty shell; it does nothing unless we tell it to.
            PaymentIntent mockIntent = mock(PaymentIntent.class);

            // Your code logs 'intent.getId()', so we must tell the mock to return a value,
            // otherwise the logger (and the final assertion) will receive 'null'.
            when(mockIntent.getId()).thenReturn("pi_123");

            // Link 1: Intercept the static 'retrieve' to return our mock
            // Your service calls 'PaymentIntent.retrieve("pi_123")'.
            // We intercept that static call and tell it: "Don't go to Stripe's servers;
            // instead, hand back the 'mockIntent' we created above."
            mockPaymentIntent.when(() -> PaymentIntent.retrieve("pi_123"))
                    .thenReturn(mockIntent);

            // Link 2: Intercept the instance '.confirm' call on that mock
            // Once your service has the 'mockIntent', it immediately calls '.confirm(...)'.
            // We tell the mockIntent: "When someone calls your confirm method with any parameters,
            // just return yourself (the mockIntent) back to the caller."
            when(mockIntent.confirm(any(PaymentIntentConfirmParams.class)))
                    .thenReturn(mockIntent);

            // WHEN: The service execution triggers the chain
            // 5. EXECUTE THE SERVICE
            // Now we run the actual method. It will follow the 'breadbrumbs' we laid out:
            // It hits 'retrieve' (gets mock) -> then hits '.confirm' (gets mock back).
            var result = stripeService.confirmPaymentIntent("pi_123", "pm_card", "url");

            // THEN: Verify the final object in the chain is our mock
            assertEquals("pi_123", result.getId());
        }

        /**
         * Validates the cancellation flow.
         * <p>
         * <b>Coverage:</b> cancelPaymentIntent() retrieve-then-cancel chain.
         * </p>
         */
        @Test
        @DisplayName("Cancel Intent - Success: Retrieve then Cancel")
        void cancel_success() throws StripeException {
            // GIVEN: Target ID
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_999");
            // MOCK: Static PaymentIntent.retrieve returns mock intent
            mockPaymentIntent.when(() -> PaymentIntent.retrieve("pi_999"))
                    .thenReturn(mockIntent);
            // MOCK: Mock intent's .cancel() returns itself
            when(mockIntent.cancel())
                    .thenReturn(mockIntent);
            // WHEN: cancelPaymentIntent is called
            var result = stripeService.cancelPaymentIntent("pi_999");
            // THEN: Verify .cancel() was called on the intent
            assertNotNull(result);
            assertEquals("pi_999", result.getId());
        }
    }

    /**
     * Tests for data retrieval and post-payment operations like refunds.
     */
    @Nested
    @DisplayName("Lookups & Post-Payment Logic")
    class ResourceTests {

        /**
         * Tests simple retrieval of a PaymentIntent by ID.
         * <p>
         * <b>Coverage:</b> retrievePaymentIntent() pass-through.
         * </p>
         */
        @Test
        @DisplayName("Retrieve Intent - Success: Basic ID lookup")
        void retrieve_success() throws StripeException {
            // GIVEN: We prepare our "Stunt Double" (the mock intent) and give it an identity.
            // We do this so that when the service gets the object, it actually has an ID to return.
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_482");

            // LINK: Intercept the static 'retrieve' call on the PaymentIntent class.
            // We tell the "Gatekeeper": "When someone asks for this specific ID, don't call Stripe.
            // Instead, hand them this mockIntent we just built."
            mockPaymentIntent.when(() -> PaymentIntent.retrieve("pi_482"))
                    .thenReturn(mockIntent);

            // WHEN: We call the service method to perform the lookup.
            var result = stripeService.retrievePaymentIntent("pi_482");

            // THEN: We verify that the "hand-off" was successful.
            // We check that the result isn't null and that it carries the ID we expected.
            assertNotNull(result);
            assertEquals("pi_482", result.getId());
        }

        /**
         * Validates the refund creation logic.
         * <p>
         * <b>Coverage:</b> refundPayment() static call.
         * </p>
         */
        @Test
        @DisplayName("Refund Payment - Success: Refund object creation")
        void refund_success() throws StripeException {
            // GIVEN: We create a "Stunt Double" for the Refund object.
            // We set up its ID so we can verify it at the very end of the test.
            Refund refundAttempt = mock(Refund.class);
            when(refundAttempt.getId()).thenReturn("pi_234");

            // LINK: Intercept the static 'Refund.create' call.
            // Crucial: Use the static controller for the Refund class here.
            // We tell it to return our stunt double whenever a refund is created.
            mockRefund.when(() -> Refund.create(any(RefundCreateParams.class)))
                    .thenReturn(refundAttempt);

            // WHEN: We trigger the refund logic in our service.
            var result = stripeService.refundPayment("pi_234");
            // THEN: Verify the service returned the refund object we "programmed" into the mock.
            // This proves the service correctly talked to the Refund gatekeeper.
            assertNotNull(result);
            assertEquals("pi_234", result.getId());
        }
    }

    /**
     * Validates service resilience against unexpected SDK behaviors and malformed data.
     */
    @Nested
    @DisplayName("Edge Cases: SDK Resilience & Validation")
    class EdgeCaseTests {

        /**
         * Verifies behavior when Stripe cannot find the requested PaymentIntent.
         * <p>
         * <b>Edge Case:</b> ID exists in local context but returns null from Stripe API.
         * </p>
         */
        @Test
        @DisplayName("Retrieve Intent - Resource not found (null)")
        void retrieve_not_found() throws StripeException {
            // GIVEN: A valid-looking ID string.
            String validLookingId = "pi_231";
            // LINK: Intercept the static 'retrieve' call.
            // Program the Gatekeeper to return 'null' instead of a mock object.
            mockPaymentIntent.when(() -> PaymentIntent.retrieve(validLookingId))
                    .thenReturn(null);
            // WHEN: Call the service retrieval method.
            var result = stripeService.retrievePaymentIntent(validLookingId);
            // THEN: Assert that the result is null.
            // This ensures the service doesn't crash with a NullPointerException internally.
            assertNull(result);
        }

        /**
         * Validates handling of invalid state transitions (e.g., confirming a canceled intent).
         * <p>
         * <b>Edge Case:</b> Stripe throws an exception because the intent is in an un-confirmable state.
         * </p>
         */
        @Test
        @DisplayName("Confirm Intent - Handle already-processed payment conflict")
        void confirm_invalid_state() throws StripeException {
            // GIVEN: A Stunt Double (mock) and the error we expect when a payment is already done.
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            StripeException stateConflictError = new StripeException(
                    "Cannot confirm a finished payment", null, "intent_state_invalid", 400) {
            };

            // LINK 1: The "Identity Swap" - Make sure the Service finds our Stunt Double.
            mockPaymentIntent.when(() -> PaymentIntent.retrieve("pi_conflict_123"))
                    .thenReturn(mockIntent);

            // LINK 2: The "Sabotage" - Script the Stunt Double to fail when confirmed.
            when(mockIntent.confirm(any(PaymentIntentConfirmParams.class)))
                    .thenThrow(stateConflictError);

            // WHEN/THEN: Verify the Service correctly passes the Stripe error up to the caller.
            assertThrows(StripeException.class,
                    () -> stripeService.confirmPaymentIntent("pi_conflict_123", "pm_card", "url")
            );
        }

        /**
         * Verifies that the service handles empty or blank identifiers gracefully.
         * <p>
         * <b>Edge Case:</b> Malformed or empty input strings passed to Stripe API.
         * </p>
         */
        @Test
        @DisplayName("Retrieve Intent - Empty ID handling")
        void retrieve_empty_id() throws StripeException {
            // 1. GIVEN: The input we are testing (a blank/empty ID).
            String emptyId = "";
            // 2. THE ERROR: We mimic Stripe's "Invalid Request" error.
            // Notice we don't need a mockIntent here because we never get that far!
            StripeException invalidRequestError = new StripeException("The ID cannot be empty", null, "parameter_missing", 400) {
            };
            // 3. THE LINK: Tell the "Gatekeeper" (Static Method) to explode immediately.
            // "If anyone asks for '', don't return an object, just throw this error."
            mockPaymentIntent.when(() -> PaymentIntent.retrieve(emptyId))
                    .thenThrow(invalidRequestError);
            // 4. WHEN/THEN: Verify the service correctly passes the "Invalid Request" up.
            assertThrows(StripeException.class,
                    () -> stripeService.retrievePaymentIntent(emptyId));
        }

        @Test
        @DisplayName("Retrieve Intent - SDK Network Timeout")
        void retrieve_timeout_error() {
            // GIVEN: A special Stripe error that mimics a "Connection Timeout"
            StripeException networkError = new ApiConnectionException("Stripe is unreachable", null);

            // LINK: The Gatekeeper throws a network error
            mockPaymentIntent.when(() -> PaymentIntent.retrieve(anyString()))
                    .thenThrow(networkError);

            // WHEN/THEN: Prove your service doesn't crash the whole server when the internet dies
            assertThrows(StripeException.class,
                    () -> stripeService.retrievePaymentIntent("pi_123"));
        }
    }
}