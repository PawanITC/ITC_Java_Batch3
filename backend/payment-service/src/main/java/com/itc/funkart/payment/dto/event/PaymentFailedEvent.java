package com.itc.funkart.payment.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class PaymentFailedEvent {
    @JsonProperty("payment_id")
    private Long paymentId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("order_id")
    private Long orderId;

    private BigDecimal amount;
    private String currency;

    @JsonProperty("failure_reason")
    private String failureReason;

    @JsonProperty("stripe_error_code")
    private String stripeErrorCode;

    private Long timestamp;

    public PaymentFailedEvent() {}

    public PaymentFailedEvent(Long paymentId, Long userId, Long orderId,
                              BigDecimal amount, String currency,
                              String failureReason, String stripeErrorCode, Long timestamp) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.failureReason = failureReason;
        this.stripeErrorCode = stripeErrorCode;
        this.timestamp = timestamp;
    }

}