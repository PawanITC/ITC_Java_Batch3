package com.itc.funkart.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long userId,
        Long orderId,
        BigDecimal amount,
        String currency,
        String status,  // "pending", "succeeded", "failed", "refunded"
        LocalDateTime createdAt
) {}