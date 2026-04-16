package com.example.notificationservice;

import com.example.notificationservice.event.OrderStatus;
import com.example.notificationservice.template.MessageBuilderTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MessageBuilderTemplateTests {

    @Test
    void validateMessageBuilderTemplateGenerateMessage() {

        String message = MessageBuilderTemplate.generateMessage("123", OrderStatus.ORDER_CONFIRMED);

        Assertions.assertEquals("Good news! Your order #123 has been confirmed.", message);
    }

    @Test
    void validateMessageBuilderTemplateGenerateSubject() {
        String message = MessageBuilderTemplate.generateSubject("789989", OrderStatus.DELIVERED);

        Assertions.assertEquals("Update for your Funkart order #"+"789989 Status: Order delivered!", message);
    }
}
