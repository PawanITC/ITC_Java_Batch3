package com.itc.catalogueservice.exception;

import com.itc.catalogueservice.exception.catalogue.NoProductsException;
import com.itc.catalogueservice.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(NoProductsException.class)
    public ResponseEntity<ApiResponse<?>> handleNoProductsException(NoProductsException ex) {

        log.error("Products not available");

        ApiResponse<?> response =
                new ApiResponse<>(HttpStatus.NOT_FOUND.value(), ex.getMessage(), null);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
