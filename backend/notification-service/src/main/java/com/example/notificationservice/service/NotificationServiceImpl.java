package com.example.notificationservice.service;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.template.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository repository;
    private final MockEmailSender mockEmailSender;
    private final MockSmsSender mockSmsSender;
    private final SmtpEmailSender smtpEmailSender;
    private final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    public NotificationServiceImpl(NotificationRepository repository,
                                   MockEmailSender mockEmailSender,
                                   MockSmsSender mockSmsSender ,SmtpEmailSender smtpEmailSender) {

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

        String message = MessageBuilderTemplate.generateMessage(event.getOrderId(), event.getStatus());//message body in email
        String subject = MessageBuilderTemplate.generateSubject(event.getOrderId(), event.getStatus());//subject header for email

        if (event.getEmail() != null && !event.getEmail().isEmpty()) {//making sure email field is present etc.

            try {
                mockEmailSender.sendEmail(event.getEmail(),subject, message);//then sends the notification via email/sms
                smtpEmailSender.sendEmail(event.getEmail(), subject, message);//send the real e-mail using smtp
            }catch (Exception e) {
                log.error("Failed to send email for order {} : {}", event.getOrderId(), e.getMessage());
            }

        }
        //always tyy sending to both channels
        if (event.getPhone() != null && !event.getPhone().isEmpty()) {

            try{
            mockSmsSender.sendSms(event.getPhone(), message);
            }catch (Exception e) {
                log.error("Failed to send sms for order {} : {}", event.getOrderId(), e.getMessage());

            }
    }
    }

    private String buildMessage(OrderEventDTO event) {

        return "Order " + event.getOrderId() + " status: " + event.getStatus();

    }
}
