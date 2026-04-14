package com.example.notificationservice.service;

import com.example.notificationservice.exception.FailedToSendEmailException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;

//@Profile("prod")
@Service
public class SmtpEmailSender implements EmailSender {//uses the SMTP implemented via javamailSender
    private final JavaMailSender mailSender;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Observed(name = "Smtp-send-email")
    @Override
    public void sendEmail(String email, String Subject, String message) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("a.khan480849@gmail.com");
        msg.setTo(email);
        msg.setSubject(Subject);
        msg.setText(message);
        try{
        mailSender.send(msg);}
        catch (MailException ex){//dont just catch any error only catch this specific mail error
            throw new FailedToSendEmailException("Error Encountered! Failed To Send Email to: "+email+", "+ex.getMessage());//rethrow as our own exception
        }
    }
}
