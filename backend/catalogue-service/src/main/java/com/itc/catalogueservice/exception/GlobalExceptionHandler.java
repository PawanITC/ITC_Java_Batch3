package com.itc.catalogueservice.exception;

import com.itc.catalogueservice.exception.catalogue.NoProductsException;
import com.itc.catalogueservice.exception.external.ExternalServiceFailureException;
import com.itc.catalogueservice.exception.external.ExternalServiceTimeoutException;
import com.itc.catalogueservice.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    //Handles no products
    @ExceptionHandler(NoProductsException.class)
    public ResponseEntity<ApiResponse<?>> handleNoProductsException(NoProductsException ex) {

        log.error("Products not available",ex);

        ApiResponse<?> response =
                new ApiResponse<>(HttpStatus.NOT_FOUND.value(), ex.getMessage(), null);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }


    //Handles constraints
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {

        log.error("Validation failed",ex);

        ApiResponse<?> response =
                new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
                        ex.getConstraintViolations().iterator().next().getMessage(),
                        null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


    //Timeout for product API client error message: 504
    @ExceptionHandler(ExternalServiceTimeoutException.class)
    public ResponseEntity<ApiResponse<?>> handleTimeout(ExternalServiceTimeoutException ex) {

        log.error("External service timeout", ex);

        ApiResponse<?> response =
                new ApiResponse<>(HttpStatus.GATEWAY_TIMEOUT.value(), ex.getMessage(), null);

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
    }


    // Product API client failure → 503
    @ExceptionHandler(ExternalServiceFailureException.class)
    public ResponseEntity<ApiResponse<?>> handleExternalFailure(ExternalServiceFailureException ex) {

        log.error("External service failure", ex);

        ApiResponse<?> response =
                new ApiResponse<>(HttpStatus.SERVICE_UNAVAILABLE.value(), ex.getMessage(), null);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
