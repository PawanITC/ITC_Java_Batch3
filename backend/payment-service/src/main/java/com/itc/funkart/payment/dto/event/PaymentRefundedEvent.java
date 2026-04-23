package com.itc.funkart.payment.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.itc.funkart.payment.entity.Payment;
import com.stripe.model.Charge;
import com.stripe.model.Refund;

/**
 * Unified Kafka Event published whenever a refund is successfully processed.
 * <p>
 * <b>Source Versatility:</b> This event can be triggered by either a manual
 * admin action (via the Refund object) or an asynchronous Stripe Webhook
 * (via the Charge object).
 * </p>
 * <p>
 * <b>Downstream Impact:</b> Used by the Order Service to restock inventory
 * and the Accounting Service to balance the ledger.
 * </p>
 */
public record PaymentRefundedEvent(
        @JsonProperty("payment_id") Long paymentId,
        @JsonProperty("order_id") Long orderId,
        @JsonProperty("stripe_refund_id") String stripeRefundId,
        Long amountRefunded, // The actual amount Stripe sent back
        String currency,
        Long timestamp
) {
    /**
     * Factory for Manual Refunds initiated from our internal Admin/Service layer.
     * Use this when you have the direct {@link Refund} response from the Stripe API.
     */
    public static PaymentRefundedEvent from(Payment payment, Refund stripeRefund) {

        return new PaymentRefundedEvent(
                payment.getId(),
                payment.getOrderId(),
                stripeRefund.getId(),
                stripeRefund.getAmount(),
                payment.getCurrency(),
                System.currentTimeMillis()
        );
    }

    /**
     * Factory for Webhook-driven refunds (e.g., 'charge.refunded').
     * <p>
     * <b>Logic:</b> Navigates the {@link Charge} object's refund list.
     * Defaults to 'unknown_refund_id' if the list is unexpectedly empty to
     * prevent NullPointerExceptions during Kafka publishing.
     * </p>
     */
    public static PaymentRefundedEvent from(Payment payment , Charge charge){
        String refundId = (charge.getRefunds() != null && !charge.getRefunds().getData().isEmpty())
                ? charge.getRefunds().getData().get(0).getId()
                : "unknown_refund_id";
        return new PaymentRefundedEvent(
                payment.getId(),
                payment.getOrderId(),
                refundId,
                charge.getAmountRefunded(),
                payment.getCurrency(),
                System.currentTimeMillis()
        );
    }
}
