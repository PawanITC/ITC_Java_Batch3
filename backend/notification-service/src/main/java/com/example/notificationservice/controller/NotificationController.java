package com.example.notificationservice.controller;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.response.ApiResponse;
import com.example.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {//dependency injection for spring
        this.notificationService = notificationService;
    }


    @PostMapping("/order-event")
    public ResponseEntity<ApiResponse<Void>> receiveOrderEvent(@RequestBody @Valid/*input validation*/ OrderEventDTO event) {

        notificationService.processOrderEvent(event);
        return ResponseEntity.ok(new ApiResponse<>(null,"order event processed successfully"));
    }
}
//