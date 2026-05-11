package com.itc.funkart.notification.service;

public interface EmailSender {
    public void sendEmail(String email, String Subject, String message);
}
