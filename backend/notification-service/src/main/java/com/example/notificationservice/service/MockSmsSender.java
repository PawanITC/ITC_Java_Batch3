package com.example.notificationservice.service;

import org.springframework.stereotype.Service;

@Service
public class MockSmsSender {//mock prototype classes
    public void sendSms(String phone, String message) {

        System.out.println("Sending SMS to: " + phone);
        System.out.println("Message: " + message);

    }
}
