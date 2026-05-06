package com.itc.funkart.payment.mapper;

import com.itc.funkart.common.dto.event.order.OrderCreatedEvent;
import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import org.springframework.stereotype.Component;

/**
 * <h2>PaymentEventMapper</h2>
 * <p>
 * Centralized utility for transforming external Kafka events into local domain DTOs.
 * This mapping layer acts as an "Anti-Corruption Layer" (ACL), ensuring that
 * changes in the Order Service's event structure don't break the Payment Service's
 * internal logic.
 * </p>
 */
@Component
public class PaymentEventMapper {

    /**
     * Transforms an {@link OrderCreatedEvent} into a {@link JwtUserDto}.
     * <p>
     * This allows the {@code PaymentService} to emulate an authenticated
     * user context using the data provided by the Order Service.
     * </p>
     *
     * @param event The source event from Kafka.
     * @return A populated User DTO for security context emulation.
     */
    public JwtUserDto toUserDto(OrderCreatedEvent event) {
        if (event == null) return null;

        return JwtUserDto.builder()
                .id(event.userId())
                .name(event.userName())
                .email(event.userEmail())
                .role(event.userRole())
                .build();
    }

    /**
     * Transforms an {@link OrderCreatedEvent} into a {@link ConfirmPaymentRequest}.
     * <p>
     * Extracts the Stripe {@code paymentIntentId} and {@code paymentMethodId}
     * required to finalize the transaction in the Payment Service.
     * </p>
     *
     * @param event The source event from Kafka.
     * @return A request DTO ready for processing.
     */
    public ConfirmPaymentRequest toConfirmRequest(OrderCreatedEvent event) {
        if (event == null) return null;

        return new ConfirmPaymentRequest(
                event.paymentIntentId(),
                event.paymentMethodId(),
                event.returnUrl()
        );
    }
}