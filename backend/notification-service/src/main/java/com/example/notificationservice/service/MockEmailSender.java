package com.example.notificationservice.service;

import org.springframework.stereotype.Service;

@Service
public class MockEmailSender {//mock prototype classes
    public void sendEmail(String email, String message) {

        System.out.println("Sending email to: " + email);
        System.out.println("Message: " + message);

    }
}
