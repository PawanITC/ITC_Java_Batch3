package com.itc.funkart.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

    @Entity
    @Table(name = "orders")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class Order {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID orderId;

        private UUID customerId;

        private UUID productId;

        private Integer quantity;

        private Double price;

        private String orderStatus;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;
    }

