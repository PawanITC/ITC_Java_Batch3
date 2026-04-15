package com.example.notificationservice.service;

public interface EmailSender {
    public void sendEmail(String email, String Subject, String message);
}
