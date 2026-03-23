package com.example.notificationservice.service;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.template.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
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
    private final TwilioSmsSender twilioSmsSender;

    public NotificationServiceImpl(NotificationRepository repository,
                                   MockEmailSender mockEmailSender,
                                   MockSmsSender mockSmsSender , SmtpEmailSender smtpEmailSender, TwilioSmsSender twilioSmsSender) {

        this.repository = repository;
        this.mockEmailSender = mockEmailSender;
        this.mockSmsSender = mockSmsSender;
        this.smtpEmailSender = smtpEmailSender;
        this.twilioSmsSender = twilioSmsSender;
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
                sendEmailWithRetry(event.getEmail(),subject, message);
            }catch (Exception e) {
                log.error("Failed to send email for order {} : {}", event.getOrderId(), e.getMessage());
            }

        }
        //always try sending to both channels
        if (event.getPhone() != null && !event.getPhone().isEmpty()) {

            try{
            mockSmsSender.sendSms(event.getPhone(), message);
            sendSmsWithRetry(event.getPhone(),message);
            }catch (Exception e) {
                log.error("Failed to send sms for order {} : {}", event.getOrderId(), e.getMessage());

            }
    }
    }

    //adding logic to retry with resilience4j should request fail (implemented retry here ,
    // rather than SmtpEmailSender because of following separation of concern)
    @Retry(name="emailRetry", fallbackMethod="emailFallback")
    private void sendEmailWithRetry(String email, String subject, String message) {
        smtpEmailSender.sendEmail(email, subject, message);//send the real e-mail using smtp


    }

    @Retry(name="smsRetry", fallbackMethod="smsFallback")
    @CircuitBreaker(name = "smsCircuit", fallbackMethod = "smsFallback")
    @TimeLimiter(name = "smsTimeout", fallbackMethod = "smsFallback")
    private void sendSmsWithRetry(String phone, String message) {
        twilioSmsSender.sendSms(phone, message);
    }

     //🔁 Fallback for email
     public void emailFallback(String email, String subject, String message, Exception ex) {
        System.out.println("❌ Email failed after retries: " + ex.getMessage());
         //You can also save FAILED status in DB here
    }

    // 🔁 Fallback for SMS
    public void smsFallback(String phone, String message, Exception ex) {
        System.out.println("❌ SMS failed after retries: " + ex.getMessage());
        // Save FAILED status here
    }
}
