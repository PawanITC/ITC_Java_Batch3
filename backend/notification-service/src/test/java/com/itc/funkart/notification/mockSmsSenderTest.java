package com.itc.funkart.notification;

import com.itc.funkart.notification.service.MockSmsSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class mockSmsSenderTest {

    @Test
    void mockSmsSenderPrintsToConsoleWithVariableInfo() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(baos));

        new MockSmsSender().sendSms("07890903948", "your order is cancelled!");

        System.setOut(original);
        String output = baos.toString();

        Assertions.assertTrue(output.contains("Sending SMS to: 07890903948"));
        Assertions.assertTrue(output.contains("Message: your order is cancelled!"));
    }
}
