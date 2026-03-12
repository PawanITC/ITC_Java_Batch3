package com.itc.funkart.product_service.exceptions;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApiError {

    private String message;

    private int status;

    private LocalDateTime timestamp;
}
