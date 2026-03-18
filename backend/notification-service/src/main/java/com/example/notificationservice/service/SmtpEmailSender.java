package com.example.notificationservice.service;

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


    @Override
    public void sendEmail(String email, String Subject, String message) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("a.khan480849@gmail.com");
        msg.setTo(email);
        msg.setSubject(Subject);
        msg.setText(message);
        mailSender.send(msg);
    }
}
