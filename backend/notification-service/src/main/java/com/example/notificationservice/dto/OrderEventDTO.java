package com.example.notificationservice.dto;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderEventDTO {

        private String orderId;
        private String email;
        private String phone;
        private String status;

}

