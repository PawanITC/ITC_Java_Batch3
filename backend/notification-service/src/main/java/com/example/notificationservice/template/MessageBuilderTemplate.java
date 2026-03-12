package com.example.notificationservice.template;

import com.example.notificationservice.event.OrderStatus;

public class MessageBuilderTemplate {
    public static String generateMessage(String orderId, OrderStatus status) {

        return switch (status) {
            case ORDER_PLACED -> "Your order #" + orderId + " has been placed successfully.";
            case ORDER_CONFIRMED -> "Good news! Your order #" + orderId + " has been confirmed.";
            case DISPATCHED -> "Your order #" + orderId + " has been dispatched and is on its way.";
            case OUT_FOR_DELIVERY -> "Your order #" + orderId + " is out for delivery today.";
            case DELIVERED -> "Your order #" + orderId + " has been delivered. Enjoy!";
            default -> "Update for your order #" + orderId;
        };
    }

    //TODO implement email subject message builder function
}
