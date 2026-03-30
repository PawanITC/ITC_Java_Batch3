package com.example.notificationservice.service;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.model.NotificationErrorMessages;
import com.example.notificationservice.model.SentStatus;
import com.example.notificationservice.repository.NotificationErrorRepository;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.template.*;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;


@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository repository;
    private final NotificationErrorRepository errorRepository;
    private final MockEmailSender mockEmailSender;
    private final MockSmsSender mockSmsSender;
    private final SmtpEmailSender smtpEmailSender;
    private final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final TwilioSmsSender twilioSmsSender;

    public NotificationServiceImpl(NotificationRepository repository, NotificationErrorRepository errorRepository,
                                   MockEmailSender mockEmailSender,
                                   MockSmsSender mockSmsSender , SmtpEmailSender smtpEmailSender, TwilioSmsSender twilioSmsSender) {

        this.repository = repository;
        this.errorRepository = errorRepository;
        this.mockEmailSender = mockEmailSender;
        this.mockSmsSender = mockSmsSender;
        this.smtpEmailSender = smtpEmailSender;
        this.twilioSmsSender = twilioSmsSender;
    }

    @Override
    public Notification getNotification() {
        return notification;
    }

    private Notification notification;

    @Override
    public void processOrderEvent(OrderEventDTO event) {

        notification = generateNotification(event);

        String message = MessageBuilderTemplate.generateMessage(event.getOrderId(), event.getStatus());//message body in email
        String subject = MessageBuilderTemplate.generateSubject(event.getOrderId(), event.getStatus());//subject header for email

        if (event.getEmail() != null && !event.getEmail().isEmpty()) {//making sure email field is present etc.

            try {
                mockEmailSender.sendEmail(event.getEmail(),subject, message);//then sends the notification via email/sms
                sendEmailWithRetry(event.getEmail(),subject, message);
                notification.setEmailSentStatus(SentStatus.SENT);//if we don't get any errors it means the email was sent successfully,
                // so we can update the status parameter
            }catch (Exception e) {
                log.error("Failed to send email for order {} : {}", event.getOrderId(), e.getMessage());
                if(errorRepository.existsByOrderIdAndOrderStatus(event.getOrderId(), event.getStatus())){//we'll check first if the error record already
                    //exists in the database, if so we'll just update the record .
                    Optional<NotificationErrorMessages> errorRecord = errorRepository.findByOrderIdAndOrderStatus(event.getOrderId(), event.getStatus());
                    errorRecord.get().setEmailErrorMessage(e.getMessage());
                    errorRepository.save(errorRecord.get());//update the missing parameter
                }else{//otherwise well create a new error record
                errorRepository.save(new NotificationErrorMessages(event.getOrderId(), e.getMessage(),null,event.getStatus()));
                }//
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
            }catch (Exception e) {
                log.error("Failed to send sms for order {} : {}", event.getOrderId(), e.getMessage());
                if(errorRepository.existsByOrderIdAndOrderStatus(event.getOrderId(), event.getStatus())) {//we'll check first if the error record already
                    //exists in the database, if so we'll just update the record .
                    Optional<NotificationErrorMessages> errorRecord = errorRepository.findByOrderIdAndOrderStatus(event.getOrderId(), event.getStatus());
                    errorRecord.get().setSmsErrorMessage(e.getMessage());
                    errorRepository.save(errorRecord.get());//update the missing parameter
                }else {//otherwise well create a new error record
                    errorRepository.save(new NotificationErrorMessages(event.getOrderId(), null,e.getMessage(), event.getStatus()));
                }

                notification.setSmsSentStatus(SentStatus.FAILED);//we can set the status as failed since we caught an error which means the sms did not
                //go through
            }
    }
        repository.save(notification);//saves notification log to database
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
