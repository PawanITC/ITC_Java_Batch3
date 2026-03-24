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
            case ORDER_UPDATED ->  "Great news! Your order #" + orderId + " has been updated successfully as per your request. please find the updated information" +
                    "attached below.";
            case ORDER_CANCELLED ->   "Sorry but you your order #" + orderId + " has been cancelled. This maybe because the order was cancelled by you " +
                    "otherwise we are either out of stock for your requested item(s). We will issue a refund/replacement accordingly." +
                    " Sorry for the inconvenience this may have caused you.";
            default -> "Update for your order #" + orderId;
        };
    }

    public static String generateSubject(String orderId, OrderStatus status){

        String temp;

        switch (status) {
            case ORDER_PLACED -> temp = orderId + " Status: Order placed!";
            case ORDER_CONFIRMED -> temp = orderId+" Status: Order confirmed!";
            case DISPATCHED -> temp = orderId+" Status: Order dispatched!";
            case OUT_FOR_DELIVERY -> temp = orderId+" Status: Out for delivery!";
            case DELIVERED -> temp = orderId+" Status: Order delivered!";
            default -> temp = orderId;
        }

        return "Update for your Funkart order #"+temp;
    }

}
