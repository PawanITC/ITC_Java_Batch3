package com.itc.funkart.payment.dto.response;

/**
 * Data Transfer Object (DTO) sent to the Frontend to initialize the Stripe Payment Sheet.
 * * <p>DESIGN NOTES:</p>
 * <ul>
 * <li><b>clientSecret:</b> A temporary, secure key used by Stripe.js to confirm the payment
 * without exposing your Secret API Key.</li>
 * <li><b>paymentIntentId:</b> The unique 'pi_xxx' identifier used to poll status or
 * link the transaction to a local database 'Payment' entity.</li>
 * <li><b>status:</b> The current state of the intent (e.g., 'requires_payment_method').</li>
 * </ul>
 */
public record PaymentIntentResponse(
        String clientSecret,
        String paymentIntentId,
        String status
) {
    /**
     * Static factory to map a Stripe SDK PaymentIntent to our API response.
     * Extracts only the fields necessary for the Frontend to complete the transaction.
     * * @param intent The Stripe SDK PaymentIntent object.
     * @return A sanitized DTO for the client-side.
     */
    public static PaymentIntentResponse from(com.stripe.model.PaymentIntent intent) {
        return new PaymentIntentResponse(
                intent.getClientSecret(),
                intent.getId(),
                intent.getStatus()
        );
    }
}