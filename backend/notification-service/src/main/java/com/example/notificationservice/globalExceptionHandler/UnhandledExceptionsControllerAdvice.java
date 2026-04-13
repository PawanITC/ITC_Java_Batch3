package com.example.notificationservice.globalExceptionHandler;

import com.example.notificationservice.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;



@RestControllerAdvice
public class UnhandledExceptionsControllerAdvice {

    @ExceptionHandler(RuntimeException.class)//runtime exceptions unhandled
    public ResponseEntity<ApiResponse<Void>> handleRuntimeErrors(RuntimeException ex) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(null,
                "The Notification Service Has Ran Into An Unexpected Runtime Error, Please Try Later! Details: "+ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)//all other unhandled genric exceptions
    public ResponseEntity<ApiResponse<Void>> handleAllException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(null,
                "The Notification Service Has Ran Into An Unexpected Error, Please Try Later! Details: "+ex.getMessage()));
    }

    //global handler for uncaught exception of inavlid fileds such as incorrect email or no order id etc.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(null, "Error! Validation failed , Please Make sure fields are correct! "+ex.getMessage()));
    }
}
