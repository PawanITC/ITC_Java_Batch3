package com.example.notificationservice.service;

import com.example.notificationservice.dto.OrderEventDTO;

public interface NotificationService {
    void processOrderEvent(OrderEventDTO event);
}
