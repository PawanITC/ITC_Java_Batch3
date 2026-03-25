package com.itc.funkart.payment.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record PaymentCompletedEvent(
        @JsonProperty("payment_id") Long paymentId,
        @JsonProperty("user_id")Long userId,
        @JsonProperty("order_id")Long orderId,
        BigDecimal amount,
        String currency,
        Long timestamp
) {}