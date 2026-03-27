package com.example.notificationservice.controller;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.model.Notification;
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
    public ResponseEntity<ApiResponse<Notification>> receiveOrderEvent(@RequestBody @Valid/*input validation*/ OrderEventDTO event) {

        boolean outcome = notificationService.processOrderEvent(event);//if service layer processes everything without problems

        if (outcome) {return ResponseEntity.ok(new ApiResponse<>(notificationService.getNotification(),"Order event processed successfully!"));}//is true
        else {
            return ResponseEntity.ok(new ApiResponse<>(null,"Order event processing failed! Please check field parameters and retry!"));//if not
        }

    }
}
//