package com.example.notificationservice.service;

import com.example.notificationservice.customException.FailedToSendEmailException;
import com.example.notificationservice.customException.FailedToSendSmsException;
import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.model.SentStatus;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.template.*;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository repository;
    private final ErrorRepoQuery errorRepoQuery;
    private final MockEmailSender mockEmailSender;
    private final MockSmsSender mockSmsSender;
    private final SmtpEmailSender smtpEmailSender;
    private final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final TwilioSmsSender twilioSmsSender;

    public NotificationServiceImpl(NotificationRepository repository, ErrorRepoQuery errorRepoQuery,
                                   MockEmailSender mockEmailSender,
                                   MockSmsSender mockSmsSender , SmtpEmailSender smtpEmailSender, TwilioSmsSender twilioSmsSender) {

        this.repository = repository;
        this.errorRepoQuery = errorRepoQuery;
        this.mockEmailSender = mockEmailSender;
        this.mockSmsSender = mockSmsSender;
        this.smtpEmailSender = smtpEmailSender;
        this.twilioSmsSender = twilioSmsSender;
    }


    @Override
    public Notification processOrderEvent(OrderEventDTO event) {

        Notification notification = generateNotification(event);

        String message = MessageBuilderTemplate.generateMessage(event.getOrderId(), event.getStatus());//message body in email
        String subject = MessageBuilderTemplate.generateSubject(event.getOrderId(), event.getStatus());//subject header for email

        if (event.getEmail() != null && !event.getEmail().isEmpty()) {//making sure email field is present etc.

            try {
                mockEmailSender.sendEmail(event.getEmail(),subject, message);//then sends the notification via email/sms
                sendEmailWithRetry(event.getEmail(),subject, message);
                notification.setEmailSentStatus(SentStatus.SENT);//if we don't get any errors it means the email was sent successfully,
                // so we can update the status parameter
            }catch (FailedToSendEmailException e) {//we're only concerned with this specific error to catch thrown by smtp server, any other error thrown by unrelated events can be handled by global handler
                log.error("Failed to send email for order {} : {}", event.getOrderId(), e.getMessage());

                errorRepoQuery.updateEmailErrorRecord(event,e);//call the function to update error repository with error message

                notification.setEmailSentStatus(SentStatus.FAILED);//we can set the status as failed since we caught an error which means the email did not
                //go through
            }

        }
        //always try sending to both channels
        if (event.getPhone() != null && !event.getPhone().isEmpty()) {

            try{
            mockSmsSender.sendSms(event.getPhone(), message);
            sendSmsWithRetry(event.getPhone(),message);
            notification.setSmsSentStatus(SentStatus.SENT);//if we don't get any errors it means the sms was sent successfully,
                // so we can update the status parameter
            }catch (FailedToSendSmsException e) {
                log.error("Failed to send sms for order {} : {}", event.getOrderId(), e.getMessage());

                errorRepoQuery.updateSmsErrorRecord(event,e);//call the function to update error repository with error message

                notification.setSmsSentStatus(SentStatus.FAILED);//we can set the status as failed since we caught an error which means the sms did not
                //go through
            }
    }
        repository.save(notification);//saves notification log to database

        return notification;
    }

    public static Notification generateNotification(OrderEventDTO event) {
        Notification notification = new Notification();
        notification.setOrderId(event.getOrderId());
        notification.setEmail(event.getEmail());
        notification.setPhone(event.getPhone());
        notification.setStatus(event.getStatus());
        notification.setCreatedAt(Instant.now());
        return notification;
    }

    //adding logic to retry with resilience4j should request fail (implemented retry here ,
    // rather than SmtpEmailSender because of following separation of concern)
    @Retry(name="emailRetry", fallbackMethod="emailFallback")
    private void sendEmailWithRetry(String email, String subject, String message) {
        smtpEmailSender.sendEmail(email, subject, message);//send the real e-mail using smtp


    }

    @Retry(name="smsRetry" , fallbackMethod="smsFallback")
    @CircuitBreaker(name = "smsCircuit")
    @TimeLimiter(name = "smsTimeout")
    private void sendSmsWithRetry(String phone, String message) {
        twilioSmsSender.sendSms(phone, message);
    }

     //🔁 Fallback for email
     public void emailFallback(String email, String subject, String message, Exception ex) {
        System.out.println("❌ Email failed after retries: " + ex.getMessage());
       //  You can also save FAILED status in DB here
    }

    // 🔁 Fallback for SMS
    public void smsFallback(String phone, String message, Exception ex) {
        System.out.println("❌ SMS failed after retries: " + ex.getMessage());
        // Save FAILED status here
    }
}
