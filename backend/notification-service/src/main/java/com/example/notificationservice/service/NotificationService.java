package com.example.notificationservice.service;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.model.Notification;

public interface NotificationService {
    void processOrderEvent(OrderEventDTO event);

    Notification getNotification();
}
