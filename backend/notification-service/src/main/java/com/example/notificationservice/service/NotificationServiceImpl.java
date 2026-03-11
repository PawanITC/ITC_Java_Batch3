package com.example.notificationservice.service;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository repository;
    private final EmailService emailService;
    private final SmsService smsService;

    public NotificationServiceImpl(NotificationRepository repository,
                                   EmailService emailService,
                                   SmsService smsService) {

        this.repository = repository;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @Override
    public void processOrderEvent(OrderEventDTO event) {

        Notification notification = new Notification();
        notification.setOrderId(event.getOrderId());
        notification.setEmail(event.getEmail());
        notification.setPhone(event.getPhone());
        notification.setStatus(event.getStatus());

        repository.save(notification);

        String message = buildMessage(event);

        emailService.sendEmail(event.getEmail(), message);
        smsService.sendSms(event.getPhone(), message);

    }

    private String buildMessage(OrderEventDTO event) {

        return "Order " + event.getOrderId() + " status: " + event.getStatus();

    }
}
