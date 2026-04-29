package com.itc.funkart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEventResponse implements Serializable {
    private String status;
    private String message;
    private String eventId;
    private String correlationId;
}