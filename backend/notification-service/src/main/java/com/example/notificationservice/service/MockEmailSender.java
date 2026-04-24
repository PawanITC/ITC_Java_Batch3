package com.example.notificationservice.service;

import org.springframework.stereotype.Service;

@Service
public class MockEmailSender implements EmailSender{//mock prototype classes
    public void sendEmail(String email,String Subject, String message) {

        System.out.println("Sending email to: " + email);
        System.out.println("Email Subject: " + Subject);
        System.out.println("Message: " + message);

    }
}
