package com.example.notificationservice.controller;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.service.NotificationService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/order-event")
    public void receiveOrderEvent(@RequestBody OrderEventDTO event) {

        notificationService.processOrderEvent(event);

    }
}
