package com.example.notificationservice.model;

import com.example.notificationservice.event.OrderStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class NotificationErrorMessages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderId;//primary key of notification object data
    private String emailErrorMessage;
    private String smsErrorMessage;
    private OrderStatus orderStatus;

    public NotificationErrorMessages(String orderId, String emailErrorMessage, String smsErrorMessage) {
        this.orderId = orderId;
        this.emailErrorMessage = emailErrorMessage;
        this.smsErrorMessage = smsErrorMessage;
    }

    public NotificationErrorMessages(String orderId, String emailErrorMessage, String smsErrorMessage, OrderStatus orderStatus) {
        this.orderId = orderId;
        this.emailErrorMessage = emailErrorMessage;
        this.smsErrorMessage = smsErrorMessage;
        this.orderStatus = orderStatus;
    }

    public NotificationErrorMessages() {

    }
}
