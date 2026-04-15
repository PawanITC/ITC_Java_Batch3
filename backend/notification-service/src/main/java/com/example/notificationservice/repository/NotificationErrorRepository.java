package com.example.notificationservice.repository;

import com.example.notificationservice.event.OrderStatus;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.model.NotificationErrorMessages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationErrorRepository extends JpaRepository<NotificationErrorMessages, Long> {
    Boolean existsByOrderIdAndOrderStatus(String orderId, OrderStatus status);
    Optional<NotificationErrorMessages> findByOrderIdAndOrderStatus(String orderId, OrderStatus orderStatus);

}
