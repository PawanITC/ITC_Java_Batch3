package com.itc.funkart.payment.service;

import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.dto.request.CreatePaymentIntentRequest;
import com.itc.funkart.payment.entity.Payment;
import com.itc.funkart.payment.exception.PaymentException;
import com.itc.funkart.payment.repository.PaymentRepository;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PaymentService}.
 * <p>
 * These tests focus on core business logic, status transitions, idempotency, and error wrapping
 * using Mockito to isolate dependencies (Stripe, Kafka, and Database).
 * </p>
 * </p>
 * PaymentServiceTest employs a White-Box testing strategy.
 * <p>
 * We utilize JUnit 5 for execution and Mockito for dependency isolation.
 * While BDD frameworks like Cucumber focus on high-level requirements,
 * this suite focuses on granular branch coverage and state-transition validation.
 * </p>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    // 1. DATA ACCESS: Mocks the database layer to avoid real SQL execution.
    // This allows us to simulate "Found", "Not Found", or "Database Down" scenarios instantly.
    @Mock
    private PaymentRepository mockPaymentRepository;

    // 2. EXTERNAL API: Mocks our internal wrapper for the Stripe SDK.
    // Since we already tested StripeService, we treat it as a trusted collaborator
    // and only care about what it returns (or throws).
    @Mock
    private StripeService mockStripeService;

    // 3. MESSAGE BROKER: Mocks the Kafka event stream.
    // This lets us verify that 'PaymentCompleted' events are fired without needing a real Kafka cluster.
    @Mock
    private KafkaEventPublisher mockKafkaEventPublisher;

    /**
     * THE UNIT UNDER TEST:
     * Mockito takes the three @Mocks above and injects them into the PaymentService constructor.
     * This creates a "Pure" testing environment where we only measure the logic inside PaymentService.
     */
    @InjectMocks
    private PaymentService paymentService;

    /**
     * Tests for webhook success processing.
     */
    @Nested
    @DisplayName("Webhook Handling (handlePaymentSuccess)")
    class WebhookTests {

        /**
         * Tests the scenario where Stripe sends a success webhook but the transaction
         * ID is missing from our local database.
         */
        @Test
        @DisplayName("Should throw PaymentException when transaction ID is missing from database")
        void handlePaymentSuccess_NotFound() {
            var stripeId = "non_existent_id";
            var intent = mock(PaymentIntent.class);
            when(intent.getId()).thenReturn(stripeId);
            when(mockPaymentRepository.findByStripePaymentIntentId(stripeId)).thenReturn(Optional.empty());

            assertThrows(PaymentException.class, () -> paymentService.handlePaymentSuccess(intent));

            verify(mockPaymentRepository, never()).save(any());
            verify(mockKafkaEventPublisher, never()).publishPaymentCompletedEvent(any());
        }

        /**
         * Tests idempotency: ensures that already succeeded payments aren't processed twice.
         */
        @Test
        @DisplayName("Should skip processing (idempotency) if payment is already 'succeeded'")
        void handlePaymentSuccess_AlreadySucceeded() {
            var stripeId = "pi_already_done";
            var intent = mock(PaymentIntent.class);
            when(intent.getId()).thenReturn(stripeId);

            var existingPayment = new Payment(1L, 101L, 5000L, "USD");
            existingPayment.setStatus("succeeded");

            when(mockPaymentRepository.findByStripePaymentIntentId(stripeId)).thenReturn(Optional.of(existingPayment));

            paymentService.handlePaymentSuccess(intent);

            verify(mockPaymentRepository, never()).save(any());
            verify(mockKafkaEventPublisher, never()).publishPaymentCompletedEvent(any());
        }

        /**
         * Tests the webhook handler for failed payment intents.
         * <p>
         * Verifies that the payment status is updated to 'failed' in the database
         * and that the corresponding Kafka event is published.
         * </p>
         */
        @Test
        @DisplayName("Webhook: Successfully handle failed event")
        void handlePaymentFailure_Success() {
            // Arrange
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_fail");

            Payment mockPayment = new Payment();
            mockPayment.setStatus("processing");

            when(mockPaymentRepository.findByStripePaymentIntentId("pi_fail"))
                    .thenReturn(Optional.of(mockPayment));

            // Act
            paymentService.handlePaymentFailure(mockIntent);

            // Assert
            assertEquals("failed", mockPayment.getStatus());
            verify(mockKafkaEventPublisher).publishPaymentFailedEvent(any());
            verify(mockPaymentRepository).save(mockPayment);
        }

        /**
         * Tests the idempotency of the payment failure handler.
         * <p>
         * Verifies that if a payment is already marked as 'failed', the service
         * will skip the database update and will NOT publish a duplicate
         * Kafka event. This prevents redundant processing in downstream services.
         * </p>
         */
        @Test
        @DisplayName("Webhook: Skip processing if payment is already marked as failed")
        void handlePaymentFailure_AlreadyFailed() {
            // 1. ARRANGE
            String stripeId = "pi_123";
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn(stripeId);

            Payment mockPayment = new Payment();
            mockPayment.setStripePaymentIntentId(stripeId);
            mockPayment.setStatus("failed"); // THE KEY: It's already failed

            when(mockPaymentRepository.findByStripePaymentIntentId(stripeId))
                    .thenReturn(Optional.of(mockPayment));

            // 2. ACT
            paymentService.handlePaymentFailure(mockIntent);

            // 3. ASSERT
            // Verify that we NEVER called save or published an event
            verify(mockPaymentRepository, never()).save(any(Payment.class));
            verify(mockKafkaEventPublisher, never()).publishPaymentFailedEvent(any());
        }

        /**
         * Tests the scenario where a refund event is received for a payment
         * not present in our database.
         */
        @Test
        @DisplayName("Webhook: Should throw exception if refund ID not found")
        void handlePaymentRefunded_NotFound() {
            Charge mockCharge = mock(Charge.class);
            when(mockCharge.getPaymentIntent()).thenReturn("pi_unknown");
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_unknown"))
                    .thenReturn(Optional.empty());

            assertThrows(PaymentException.class, () -> paymentService.handlePaymentRefunded(mockCharge));
        }

        /**
         * Tests idempotency for refunds.
         */
        @Test
        @DisplayName("Webhook: Skip processing if payment is already marked as refunded")
        void handlePaymentRefunded_AlreadyRefunded() {
            Charge mockCharge = mock(Charge.class);
            when(mockCharge.getPaymentIntent()).thenReturn("pi_123");

            Payment mockPayment = new Payment();
            mockPayment.setStatus("refunded"); // Already done!

            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123"))
                    .thenReturn(Optional.of(mockPayment));

            paymentService.handlePaymentRefunded(mockCharge);

            verify(mockPaymentRepository, never()).save(any());
            verify(mockKafkaEventPublisher, never()).publishPaymentRefundedEvent(any());
        }

        /**
         * Tests the standard success webhook processing.
         * Coverage: Hits the final logger.info and the end of the try block.
         */
        @Test
        @DisplayName("Webhook: Standard Success Processing")
        void handlePaymentSuccess_HappyPath() {
            // Arrange
            PaymentIntent intent = mock(PaymentIntent.class);
            when(intent.getId()).thenReturn("pi_123");
            when(intent.getAmountReceived()).thenReturn(5000L);
            when(intent.getCurrency()).thenReturn("usd");

            Payment mockPayment = new Payment();
            mockPayment.setStatus("processing"); // Not succeeded yet!

            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123"))
                    .thenReturn(Optional.of(mockPayment));

            // Act
            paymentService.handlePaymentSuccess(intent);

            // Assert
            assertEquals("succeeded", mockPayment.getStatus());
            verify(mockPaymentRepository).save(mockPayment);
            verify(mockKafkaEventPublisher).publishPaymentCompletedEvent(any());
            // The logger.info will execute now!
        }
    }

    /**
     * Tests for creating new payment intents.
     */
    @Nested
    @DisplayName("Intent Creation (createPaymentIntent)")
    class IntentCreationTests {

        /**
         * Verifies the Happy Path: Data is saved, Stripe is called, and a response is returned.
         */
        @Test
        @DisplayName("Success: Should create intent and return success response")
        void createIntent_Success() throws Exception {
            var request = new CreatePaymentIntentRequest(101L, 5000L, "usd");
            var payment = new Payment(1L, 101L, 5000L, "usd");

            var mockStripeIntent = mock(PaymentIntent.class);
            when(mockStripeIntent.getId()).thenReturn("pi_mock_123");
            when(mockStripeIntent.getClientSecret()).thenReturn("secret_123");

            when(mockPaymentRepository.save(any())).thenReturn(payment);
            when(mockStripeService.createPaymentIntent(anyLong(), anyString(), anyLong(), any()))
                    .thenReturn(mockStripeIntent);

            var response = paymentService.createPaymentIntent(1L, request);

            assertNotNull(response);
            assertEquals("pi_mock_123", response.getData().paymentIntentId());
            verify(mockPaymentRepository, times(2)).save(any());
        }

        /**
         * Verifies that generic exceptions are caught and wrapped in PaymentException.
         */
        @Test
        @DisplayName("Error: Should wrap any Stripe/System error into a PaymentException")
        void createIntent_StripeFailure() throws Exception {
            var request = new CreatePaymentIntentRequest(101L, 5000L, "usd");
            when(mockPaymentRepository.save(any())).thenReturn(new Payment());

            doThrow(new RuntimeException("API Down"))
                    .when(mockStripeService).createPaymentIntent(anyLong(), anyString(), anyLong(), anyLong());

            assertThrows(PaymentException.class, () -> paymentService.createPaymentIntent(1L, request));
        }
    }

    /**
     * Tests for Refund logic and validation helpers.
     * These tests target private helper methods through public API calls.
     */
    @Nested
    @DisplayName("Refund Logic & Helpers (refundPayment)")
    class RefundTests {

        /**
         * Tests the 'orElseThrow' branch in findPaymentByUser helper.
         */
        @Test
        @DisplayName("Branch: Should throw exception if payment ID does not exist")
        void refund_NotFound() {
            when(mockPaymentRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(PaymentException.class, () -> paymentService.refundPayment(1L, 99L));
        }

        /**
         * Validates that a user cannot refund a payment they do not own.
         * <p>
         * <b>Scenario:</b> User A attempts to refund a payment created by User B.
         * <b>Expected:</b> Throws {@link PaymentException} with "Unauthorized access to payment".
         * </p>
         */
        @Test
        @DisplayName("Branch: Unauthorized - User mismatch throws exception")
        void refund_Unauthorized() {
            // GIVEN: A payment belonging to user 999
            Payment payment = new Payment();
            payment.setId(1L);
            payment.setUserId(999L); // Explicitly set to 999

            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            // WHEN: User 1 tries to refund Payment 1
            var ex = assertThrows(PaymentException.class, () ->
                    paymentService.refundPayment(1L, 1L) // (userId, paymentId)
            );

            // THEN: Should fail at the userId check in the helper
            assertEquals("Unauthorized access to payment", ex.getMessage());
        }

        /**
         * Validates that only 'succeeded' payments are eligible for refunding.
         * <p>
         * <b>Scenario:</b> User attempts to refund a 'PENDING' or 'FAILED' payment.
         * <b>Expected:</b> Throws {@link PaymentException} with "Can only refund succeeded payments".
         * </p>
         */
        @Test
        @DisplayName("Branch: Invalid State - Payment not SUCCEEDED throws exception")
        void refund_InvalidStatus() {
            // User IDs match (1 and 1), so it passes the helper check
            var payment = new Payment(1L, 1L, 5000L, "USD");
            payment.setStatus("PENDING"); // Not SUCCEEDED

            when(mockPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            var ex = assertThrows(PaymentException.class, () ->
                    paymentService.refundPayment(1L, 1L)
            );

            assertEquals("Can only refund succeeded payments", ex.getMessage());
        }

        /**
         * Verifies that when a Stripe 'charge.refunded' event is received,
         * the system correctly identifies the payment, updates the status,
         * and notifies other services.
         */
        @Test
        @DisplayName("Webhook: Successfully transition status to 'refunded' on Stripe event")
        void handlePaymentRefunded_Success() {
            // 1. ARRANGE
            Charge mockCharge = mock(Charge.class);
            when(mockCharge.getPaymentIntent()).thenReturn("pi_123");

            Payment mockPayment = new Payment();
            mockPayment.setStatus("succeeded"); // Not refunded yet

            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123"))
                    .thenReturn(Optional.of(mockPayment));

            // 2. ACT
            paymentService.handlePaymentRefunded(mockCharge);

            // 3. ASSERT
            assertEquals("refunded", mockPayment.getStatus());
            verify(mockPaymentRepository).save(mockPayment);
            verify(mockKafkaEventPublisher).publishPaymentRefundedEvent(any());
        }

        @Test
        @DisplayName("Security: Should throw exception if user does not own the payment")
        void confirmPayment_UnauthorizedUser() {
            // --- ARRANGE ---
            Long actualOwnerId = 999L;
            Long intruderId = 123L;
            String stripeId = "pi_123";
            ConfirmPaymentRequest request = new ConfirmPaymentRequest(stripeId, "pm_card", "http://return.url");

            Payment mockPayment = new Payment();
            mockPayment.setUserId(actualOwnerId); // Belongs to someone else

            when(mockPaymentRepository.findByStripePaymentIntentId(stripeId))
                    .thenReturn(Optional.of(mockPayment));

            // --- ACT & ASSERT ---
            PaymentException ex = assertThrows(PaymentException.class,
                    () -> paymentService.confirmPayment(intruderId, request));

            assertTrue(ex.getMessage().contains("Unauthorized access"));
        }

        /**
         * Tests resilience when the database fails during the final status update.
         * Coverage: Covers the secondary repository.save() call and potential failures there.
         */
        @Test
        @DisplayName("Persistence: Should handle error if second DB save fails during creation")
        void createIntent_SecondSaveFailure() throws Exception {
            CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(999L, 5000L, "usd");
            Payment mockPayment = new Payment();
            PaymentIntent mockIntent = mock(PaymentIntent.class);

            // ADD LENIENT HERE:
            // This tells Mockito "It's okay if these aren't called in this specific failure test"
            lenient().when(mockIntent.getId()).thenReturn("pi_123");
            lenient().when(mockIntent.getClientSecret()).thenReturn("secret_123");

            // 1. Stub the repository sequence
            when(mockPaymentRepository.save(any(Payment.class)))
                    .thenReturn(mockPayment) // First call succeeds
                    .thenThrow(new RuntimeException("DB error")); // Second call fails

            // 2. Also make the Stripe service call lenient
            lenient().when(mockStripeService.createPaymentIntent(anyLong(), anyString(), anyLong(), any()))
                    .thenReturn(mockIntent);

            // 3. Act & Assert
            assertThrows(PaymentException.class, () -> paymentService.createPaymentIntent(1L, request));
        }

        /**
         * Verifies successful payment retrieval for an authorized user.
         * Coverage: getPayment() happy path.
         */
        @Test
        @DisplayName("Retrieval: Should return payment when valid user and ID provided")
        void getPayment_Success() {
            Long userId = 123L;
            Long paymentId = 1L;
            Payment mockPayment = new Payment();
            mockPayment.setId(paymentId);
            mockPayment.setUserId(userId);

            when(mockPaymentRepository.findById(paymentId))
                    .thenReturn(Optional.of(mockPayment));

            var response = paymentService.getPayment(userId, paymentId);

            assertNotNull(response);
            assertEquals("Payment retrieved successfully", response.getMessage());
            assertEquals(paymentId, response.getData().id());
        }

        /**
         * Tests the complete successful manual refund flow.
         * Coverage: Turns green everything from Stripe call to return.
         */
        @Test
        @DisplayName("Refund: Full success flow")
        void refundPayment_Success() throws Exception {
            // Arrange
            Long userId = 123L;
            Long paymentId = 1L;
            Payment mockPayment = new Payment();
            mockPayment.setId(paymentId);
            mockPayment.setUserId(userId);
            mockPayment.setStatus("succeeded"); // Must be succeeded to pass the check!
            mockPayment.setStripePaymentIntentId("pi_123");

            // Mock the "Gatekeeper"
            when(mockPaymentRepository.findById(paymentId)).thenReturn(Optional.of(mockPayment));

            // Mock Stripe
            Refund mockRefund = mock(Refund.class);
            when(mockStripeService.refundPayment(anyString())).thenReturn(mockRefund);

            // Act
            var response = paymentService.refundPayment(userId, paymentId);

            // Assert
            assertEquals("refunded", mockPayment.getStatus());
            verify(mockPaymentRepository).save(mockPayment);
            verify(mockKafkaEventPublisher).publishPaymentRefundedEvent(any());
            assertEquals("Payment refunded successfully", response.getMessage());
        }
    }

    /**
     * Focuses on "Distributed System" failures where one dependency succeeds but another fails.
     * These tests ensure the service handles partial successes without leaving data in a corrupted state.
     */
    @Nested
    @DisplayName("Edge Cases: Distributed Failures & Validation")
    class PaymentServiceEdgeCases {

        /**
         * Edge Case: Database update succeeds but Kafka event publishing fails.
         */
        @Test
        @DisplayName("Partial Success: Should throw exception if Kafka fails after DB update")
        void handlePaymentSuccess_KafkaFailure() {
            // 1. GIVEN: A payment found in the database.
            PaymentIntent mockIntent = mock(PaymentIntent.class);

            // Stub the payment intent
            when(mockIntent.getId()).thenReturn("pi_123");
            when(mockIntent.getAmountReceived()).thenReturn(5000L);
            when(mockIntent.getCurrency()).thenReturn("USD".toLowerCase());

            // 2. STUBBING THE CHAIN:
            // Link A: Stub the payment too
            Payment mockPayment = new Payment();
            mockPayment.setId(101L);
            mockPayment.setUserId(500L);
            mockPayment.setOrderId(999L);
            mockPayment.setStatus("PENDING"); // Must NOT be SUCCEEDED, or it returns early!


            // STUBBING THE REPOSITORY
            // Link B: The database must find the payment
            when(mockPaymentRepository.findByStripePaymentIntentId("pi_123"))
                    .thenReturn(Optional.of(mockPayment));
            // Link C: The database save must work
            // MOCK: Database .save() returns the updated payment successfully.
            when(mockPaymentRepository.save(any(Payment.class)))
                    .thenReturn(mockPayment);

            // MOCK: Kafka publisher .publishPaymentCompletedEvent() throws a RuntimeException.
            doThrow(new RuntimeException("Kafka Broker Unavailable"))
                    .when(mockKafkaEventPublisher)
                    .publishPaymentCompletedEvent(any());

            // WHEN/THEN: Verify that calling handlePaymentSuccess throws a PaymentException.
            // Ensure the flow is interrupted when the event cannot be sent.
            assertThrows(PaymentException.class,
                    () -> paymentService.handlePaymentSuccess(mockIntent));
        }

        /**
         * Edge Case: Internal validation passes, but Stripe API rejects the refund.
         */
        @Test
        @DisplayName("Refund Failure: Stripe API error during refund process")
        void refund_StripeApiFailure() throws Exception {
            // 1. GIVEN: Set up consistent IDs and a valid Payment state
            Long userId = 123L;
            Long paymentId = 1L;
            String stripeId = "pi_refund_123";

            Payment mockPayment = new Payment();
            mockPayment.setStripePaymentIntentId(stripeId);
            mockPayment.setUserId(userId);
            mockPayment.setStatus("SUCCEEDED"); // Required to pass the 'SUCCEEDED' check

            // 2. MOCK: Stub the repository to return our mock payment
            when(mockPaymentRepository.findById(paymentId))
                    .thenReturn(Optional.of(mockPayment));

            // 3. MOCK: Simulate Stripe rejecting the refund
            // This is the "Sabotage" line Mockito was complaining about!
            lenient().when(mockStripeService.refundPayment(stripeId))
                    .thenThrow(new InvalidRequestException("Refund failed", null, null, null, 400, null));

            // 4. WHEN/THEN: Call with CORRECT order (userId first, then paymentId)
            // The service should catch the Stripe error and wrap it in a PaymentException.
            assertThrows(PaymentException.class,
                    () -> paymentService.refundPayment(userId, paymentId));
        }

        /**
         * Edge Case: Stripe creates intent successfully, but final database update fails.
         */
        @Test
        @DisplayName("Persistence Failure: Should handle error if second DB save fails")
        void createIntent_SecondSaveFailure() throws Exception {
            // --- ARRANGE (The Setup) ---
            CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(999L, 123L, "USD");

            // Setup Stripe Mock (The Stunt Double)
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_123");

            when(mockStripeService.createPaymentIntent(
                    any(),
                    any(),
                    any(),
                    any()
                    )).thenReturn(mockIntent);

            // Setup Repository (The Trap)
            // First call: Save initial record (Success)
            // Second call: Update with Stripe ID (Explosion)
            when(mockPaymentRepository.save(any(Payment.class)))
                    .thenReturn(new Payment())
                    .thenThrow(new DataAccessException("DB Connection Lost") {});

            // --- ACT & ASSERT (The Execution and the Check) ---
            assertThrows(PaymentException.class,
                    () -> paymentService.createPaymentIntent(5000L, request),
                    "Should wrap DB failure into a PaymentException");
        }

        /**
         * Coverage: Specifically targets the generic 'catch (Exception e)' block.
         * Forces a RuntimeException to ensure that even unexpected system failures
         * are caught and wrapped in a PaymentException for the API response.
         */
        @Test
        @DisplayName("Refund: Should wrap unexpected System Errors into PaymentException")
        void refundPayment_GenericSystemFailure() {
            // 1. Arrange
            Long userId = 123L;
            Long paymentId = 1L;

            // We force the repository to throw a generic RuntimeException (not a PaymentException)
            // This bypasses the first catch block and hits the second one.
            when(mockPaymentRepository.findById(anyLong()))
                    .thenThrow(new RuntimeException("Database Connection Timeout"));

            // 2. Act & Assert
            PaymentException ex = assertThrows(PaymentException.class,
                    () -> paymentService.refundPayment(userId, paymentId));

            // Verify the wrapping logic worked
            assertTrue(ex.getMessage().contains("Refund process failed: Database Connection Timeout"));
        }
    }

    @Nested
    @DisplayName("Payment Operations")
    class OperationTests {

        /**
         * Tests that a {@link PaymentException} is thrown when a user attempts
         * to confirm a payment that does not belong to them.
         * <p>
         * Coverage: This validates the security check in the private findPaymentForUser method.
         * </p>
         */
        @Test
        @DisplayName("Security: Should throw exception if user does not own the payment")
        void confirmPayment_UnauthorizedUser() {
            // Arrange
            Long actualOwnerId = 999L;
            Long intruderId = 123L;
            String stripeId = "pi_123";
            ConfirmPaymentRequest request = new ConfirmPaymentRequest(stripeId, "pm_card", "http://return.url");

            Payment mockPayment = new Payment();
            mockPayment.setUserId(actualOwnerId);

            when(mockPaymentRepository.findByStripePaymentIntentId(stripeId))
                    .thenReturn(Optional.of(mockPayment));

            // Act & Assert
            PaymentException ex = assertThrows(PaymentException.class,
                    () -> paymentService.confirmPayment(intruderId, request));

            assertTrue(ex.getMessage().contains("Unauthorized access"));
        }

        /**
         * Tests the standard retrieval of a payment record.
         * <p>
         * Verifies that the service correctly maps User and Payment IDs
         * and returns a successful API response.
         * </p>
         */
        @Test
        @DisplayName("Retrieval: Should return payment when valid user and ID provided")
        void getPayment_Success() {
            // Arrange
            Long testUserId = 123L;
            Long testPaymentId = 1L;

            Payment mockPayment = new Payment();
            mockPayment.setId(testPaymentId);
            mockPayment.setUserId(testUserId);

            // This MUST match the ID that actually goes into findById()
            when(mockPaymentRepository.findById(testPaymentId))
                    .thenReturn(Optional.of(mockPayment));

            // Act
            // Make sure these match the Service's expected order: (userId, paymentId)
            var response = paymentService.getPayment(testUserId, testPaymentId);

            // Assert
            assertNotNull(response);
            assertEquals(testPaymentId, response.getData().id());
        }
    }

    @Nested
    @DisplayName("Payment Confirmation (confirmPayment)")
    class ConfirmationTests {

        /**
         * Happy path for confirmation.
         * Coverage: Everything inside the 'try' block.
         */
        @Test
        @DisplayName("Confirm: Should successfully initiate confirmation")
        void confirmPayment_Success() throws Exception {
            Long userId = 123L;
            String stripeId = "pi_123";
            ConfirmPaymentRequest request = new ConfirmPaymentRequest(stripeId, "pm_card", "url");

            Payment mockPayment = new Payment();
            mockPayment.setUserId(userId);

            when(mockPaymentRepository.findByStripePaymentIntentId(stripeId))
                    .thenReturn(Optional.of(mockPayment));

            var response = paymentService.confirmPayment(userId, request);

            assertNotNull(response);
            assertEquals("Payment confirmation initiated", response.getMessage());

            assertEquals("PROCESSING", mockPayment.getStatus());
            verify(mockStripeService).confirmPaymentIntent(eq(stripeId), any(), any());
            verify(mockPaymentRepository).save(mockPayment);
        }

        /**
         * Coverage: The 'catch (StripeException ex)' block.
         * Ensures that when Stripe fails, we don't crash, but wrap the error correctly.
         */
        @Test
        @DisplayName("Confirm: Should wrap Stripe API errors into PaymentException")
        void confirmPayment_StripeError() throws Exception {
            // 1. Arrange: Ensure the user matches so we get past the "Gatekeeper"
            Long userId = 123L;
            Payment mockPayment = new Payment();
            mockPayment.setUserId(userId);

            when(mockPaymentRepository.findByStripePaymentIntentId(anyString()))
                    .thenReturn(Optional.of(mockPayment));

            // 2. Sabotage: Use lenient() to prevent strict stubbing errors
            // if the logic flow exits unexpectedly.
            lenient().doThrow(new InvalidRequestException("Invalid API Key", null, null, null, null, null))
                    .when(mockStripeService).confirmPaymentIntent(any(), any(), any());

            // 3. Act & Assert
            assertThrows(PaymentException.class,
                    () -> paymentService.confirmPayment(userId, new ConfirmPaymentRequest("pi_1", "pm_1", "url")));
        }
    }
}