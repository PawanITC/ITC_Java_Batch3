package com.example.notificationservice.service;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.template.*;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository repository;
    private final MockEmailSender mockEmailSender;
    private final MockSmsSender mockSmsSender;
    private final SmtpEmailSender smtpEmailSender;

    public NotificationServiceImpl(NotificationRepository repository,
                                   MockEmailSender mockEmailSender,
                                   MockSmsSender mockSmsSender, SmtpEmailSender smtpEmailSender) {

        this.repository = repository;
        this.mockEmailSender = mockEmailSender;
        this.mockSmsSender = mockSmsSender;
        this.smtpEmailSender = smtpEmailSender;
    }

    @Override
    public void processOrderEvent(OrderEventDTO event) {

        Notification notification = new Notification();
        notification.setOrderId(event.getOrderId());
        notification.setEmail(event.getEmail());
        notification.setPhone(event.getPhone());
        notification.setStatus(event.getStatus());

        repository.save(notification);//saves notification log to database

        String message = MessageBuilderTemplate.generateMessage(event.getOrderId(), event.getStatus());

        //TODO: need to add logic to handle if email/sms not given or blank


        mockEmailSender.sendEmail(event.getEmail(), message);//then sends the notification via email/sms
        smtpEmailSender.sendEmail(event.getEmail(), "Order Update for order: "+event.getOrderId(), message);
        mockSmsSender.sendSms(event.getPhone(), message);

    }

    private String buildMessage(OrderEventDTO event) {

        return "Order " + event.getOrderId() + " status: " + event.getStatus();

    }
}
