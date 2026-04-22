package com.example.notificationservice;

import com.example.notificationservice.event.OrderStatus;
import com.example.notificationservice.template.MessageBuilderTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class MessageBuilderTemplateTests {

    @Test
    void validateMessageBuilderTemplateGenerateMessage() {

        List<String> expectedMessages = new ArrayList<>();
        expectedMessages.add("Your order #" + 123 + " has been placed successfully.");
        expectedMessages.add("Good news! Your order #123 has been confirmed.");
        expectedMessages.add("Your order #" + 123 + " has been dispatched and is on its way.");
        expectedMessages.add("Your order #" + 123 + " is out for delivery today.");
        expectedMessages.add("Your order #" + 123 + " has been delivered. Enjoy!");
        expectedMessages.add("Great news! Your order #" + 123 + " has been updated successfully as per your request. please find the updated information" +
                "attached below.");
        expectedMessages.add("Sorry but you your order #" + 123 + " has been cancelled. This maybe because the order was cancelled by you " +
                "otherwise we are either out of stock for your requested item(s). We will issue a refund/replacement accordingly." +
                " Sorry for the inconvenience this may have caused you.");

        int index = 0;

        for(OrderStatus orderStatus:OrderStatus.values()){
            String message = MessageBuilderTemplate.generateMessage("123", orderStatus);
            Assertions.assertEquals(expectedMessages.get(index), message);
            index++;
        }

    }

    @Test
    void validateMessageBuilderTemplateGenerateSubject() {
        List<String> expectedMessages = new ArrayList<>();
        expectedMessages.add("Update for your Funkart order #"+717+" Status: Order placed!");
        expectedMessages.add("Update for your Funkart order #"+717+" Status: Order confirmed!");
        expectedMessages.add("Update for your Funkart order #"+717+" Status: Order dispatched!");
        expectedMessages.add("Update for your Funkart order #"+717+" Status: Out for delivery!");
        expectedMessages.add("Update for your Funkart order #"+717+" Status: Order delivered!");
        expectedMessages.add("Update for your Funkart order #"+717);

        int index = 0;

        for(OrderStatus orderStatus:OrderStatus.values()){
            String message = MessageBuilderTemplate.generateSubject("717", orderStatus);
            Assertions.assertEquals(expectedMessages.get(index), message);

           if(index<5) index++;
        }
    }
}
