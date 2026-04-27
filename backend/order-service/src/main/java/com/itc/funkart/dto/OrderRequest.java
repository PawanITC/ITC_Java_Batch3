package com.itc.funkart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

    @Data
    @AllArgsConstructor
    public class OrderRequest {

        private String customerId;
        private String productId;
        private Integer quantity;
        private Double price;
    }

