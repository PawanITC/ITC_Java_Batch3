package com.example.notificationservice;

import com.example.notificationservice.service.MockSmsSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

@SpringBootTest
public class mockSmsSenderTest {

    @Test//the mock sms sender should print out message to console when event parameter fields are valid
    void mockSmsSenderPrintsToConsoleWithVariableInfo() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = System.out;

        System.setOut(new PrintStream(baos));//start new stream boundary

        MockSmsSender mockSmsSender = new MockSmsSender();
        mockSmsSender.sendSms("07890903948", "your order is cancelled!");

        System.setOut(ps);//reset new boundary

        String output = baos.toString();

        Assertions.assertTrue(output.contains("Sending SMS to: " + "07890903948"));
        Assertions.assertTrue(output.contains("Message: " + "your order is cancelled!"));
    }
}
