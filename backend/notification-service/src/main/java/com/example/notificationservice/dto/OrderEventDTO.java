package com.example.notificationservice.dto;


import com.example.notificationservice.event.OrderStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class OrderEventDTO {
        @NotBlank(message = "There needs to be an order ID!") //input validation
        private String orderId;
        @NotBlank(message = "There needs to be an e-mail at least!")
        @Email(message = "This should be in a valid e-mail format!")
        private String email;
        private String phone;
        @NotNull
        private OrderStatus status;

}

