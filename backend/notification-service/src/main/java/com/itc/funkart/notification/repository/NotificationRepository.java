package com.itc.funkart.notification.repository;

import com.itc.funkart.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {


    Object findByOrderId(String orderId);
}
