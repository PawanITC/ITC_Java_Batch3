package com.itc.funkart.payment.kafka.consumer;

import com.itc.funkart.common.constants.messaging.KafkaGroups;
import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.payment.entity.Payment;
import com.itc.funkart.payment.repository.PaymentRepository;
import com.itc.funkart.payment.service.StripeService;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOrderEventConsumer {

    private final PaymentRepository paymentRepository;
    private final StripeService stripeService;

    @Transactional
    @KafkaListener(
            topics = KafkaTopics.ORDERS,
            groupId = KafkaGroups.PAYMENT_SERVICE_GROUP
    )
    public void onOrderEvent(
            @Payload Map<String, Object> event,
            Acknowledgment ack
    ) {

        log.info("📥 [ORDER_EVENT] payload={}", event);

        try {

            // -----------------------------
            // EVENT TYPE (FROM PAYLOAD)
            // -----------------------------
            String eventType = (String) event.get("eventType");

            if (!"ORDER_INITIATED".equals(eventType)) {
                log.info("⏭️ Ignored event type={}", eventType);
                ack.acknowledge();
                return;
            }

            Long orderId = safeLong(event.get("orderId"));
            Long customerId = safeLong(event.get("customerId"));
            BigDecimal totalAmount = safeBigDecimal(event.get("totalAmount"));

            if (orderId == null || customerId == null || totalAmount == null) {
                log.error("❌ Invalid ORDER_INITIATED payload={}", event);
                ack.acknowledge();
                return;
            }

            if (paymentRepository.existsByOrderId(orderId)) {
                log.info("⏭️ Payment already exists orderId={}", orderId);
                ack.acknowledge();
                return;
            }

            long amountCents = totalAmount
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            Payment payment = paymentRepository.save(
                    new Payment(customerId, orderId, amountCents, "usd")
            );

            PaymentIntent intent = stripeService.createPaymentIntent(
                    amountCents,
                    "usd",
                    customerId,
                    payment.getId(),
                    payment.getOrderId()
            );

            payment.setStripePaymentIntentId(intent.getId());
            paymentRepository.save(payment);

            log.info("✅ PaymentIntent created orderId={} paymentId={}",
                    orderId, payment.getId());

        } catch (Exception e) {
            log.error("❌ ORDER_EVENT processing failed event={}", event, e);
        } finally {
            // IMPORTANT: always commit offset to prevent infinite retry loops
            ack.acknowledge();
        }
    }

    // -----------------------------
    // SAFE PARSERS
    // -----------------------------

    private Long safeLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.longValue();
        try {
            return Long.valueOf(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal safeBigDecimal(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }
}