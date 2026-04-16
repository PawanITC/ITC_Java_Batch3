package com.example.notificationservice.service;

import org.springframework.stereotype.Service;

@Service
public class MockSmsSender implements SmsSender{//mock prototype classes
    public void sendSms(String recipientNumber, String message) {

        System.out.println("Sending SMS to: " + recipientNumber);
        System.out.println("Message: " + message);

    }
}
