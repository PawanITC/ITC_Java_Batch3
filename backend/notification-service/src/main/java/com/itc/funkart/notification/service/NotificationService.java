package com.itc.funkart.notification.service;

import com.itc.funkart.notification.dto.OrderEventDTO;
import com.itc.funkart.notification.model.Notification;

public interface NotificationService {
    Notification processOrderEvent(OrderEventDTO event);

}
