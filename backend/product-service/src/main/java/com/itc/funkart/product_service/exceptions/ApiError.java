package com.itc.funkart.product_service.exceptions;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private Map<String, String> errors;
}
