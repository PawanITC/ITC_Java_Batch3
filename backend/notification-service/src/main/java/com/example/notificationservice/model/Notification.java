package com.example.notificationservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    private String orderId;
    @Setter
    private String email;
    @Setter
    private String phone;
    @Setter
    private String status;

}
