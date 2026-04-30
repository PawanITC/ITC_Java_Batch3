package com.example.notificationservice;

import com.example.notificationservice.service.MockEmailSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class mockEmailSenderTests {
    @Test//the mock email sender should print out message to console when event parameter fields are valid
    void validateMockEmailSenderPrintsToConsole() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = System.out;

        System.setOut(new PrintStream(baos));//start new stream boundary

        MockEmailSender mockEmailSender = new MockEmailSender();
        mockEmailSender.sendEmail("holymoly@gmail.com","your order update", "your order is cancelled!");

        System.setOut(ps);//reset new boundary

        String output = baos.toString();

        Assertions.assertTrue(output.contains("Sending email to: " + "holymoly@gmail.com"));
        Assertions.assertTrue(output.contains("Email Subject: " + "your order update"));
        Assertions.assertTrue(output.contains("Message: " + "your order is cancelled!"));
    }
}
