package com.example.notificationservice.model;

import com.example.notificationservice.event.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

}
