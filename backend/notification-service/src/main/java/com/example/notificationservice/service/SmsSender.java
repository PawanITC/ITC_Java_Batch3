package com.example.notificationservice.service;

public interface SmsSender {
    public void sendSms(String recipientNumber, String message);
}
