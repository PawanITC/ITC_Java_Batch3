package com.example.notificationservice.model;

import com.example.notificationservice.event.OrderStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderId;
    private String email;
    private String phone;
    private OrderStatus status;
    private SentStatus emailSentStatus;//whether the smtp server successfully sent email or encountered issue
    private SentStatus smsSentStatus;//whether the twilio server successfully sent sms or encountered issue
    private Instant createdAt;
    private Instant updatedAt;
}
