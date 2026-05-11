package com.itc.funkart.notification.service;

public interface SmsSender {
    public void sendSms(String recipientNumber, String message);
}
