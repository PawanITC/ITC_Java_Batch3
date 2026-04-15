package com.itc.catalogueservice.kafka.listener.dto;


import com.itc.catalogueservice.dto.ProductDTO;
import lombok.Data;

@Data
public class ProductEventDTO {

    private EventType eventType;   // CREATED, UPDATED, DELETED
    private ProductDTO product;    // full product payload
}