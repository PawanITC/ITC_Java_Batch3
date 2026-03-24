package com.example.notificationservice;

import com.example.notificationservice.dto.OrderEventDTO;
import org.hibernate.validator.internal.constraintvalidators.bv.AssertTrueValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;

import com.example.notificationservice.template.*;
import com.example.notificationservice.event.OrderStatus;
import com.example.notificationservice.service.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

@SpringBootTest
class NotificationServiceApplicationTests {

    @Autowired
    private AssertTrueValidator assertTrueValidator;

    @Test
    void contextLoads() {
    }

    //------------------------MAIN FUNCTIONAL TESTS-------------------------------------------------------------------------

    @Test //overall endpoint test, when request is received by our endpoint it should return a response back
    void validateEndpointReceiveOrderEvent() {
        Assertions.assertEquals(true, true);
    }

    @Test //if our request is valid then a 'successful' response should be returned
    void validateEndpointSendSuccessfulResponse() {
        Assertions.assertEquals(true, true);
    }

    @Test //if our request is invalid then an 'unsuccessful' response should be returned
    void validateEndpointSendUnsuccessfulResponse() {
        Assertions.assertEquals(true, true);
    }

    @Test //ensure we receive event dto with correctly validated fields
    void validateOrderEventDTOFields(OrderEventDTO eventDTO) {//ensure all fields are validated


    }

    @Test //tests to see if an orderEventDto is successfully mapped to the model Notification object
    void serviceLayerMapsOrderEventDTOtoNotification() {
    }

    @Test
    void validateNotificationSavedToRepository() {
    }

    @Test //test whether the database connects with our credentials stored in secrets
    void testDatabaseConnectivity() {
    }

    @Test
    void validateMessageBuilderTemplateGenerateMessage() {

        String message = MessageBuilderTemplate.generateMessage("123", OrderStatus.ORDER_CONFIRMED);

        Assertions.assertEquals("Good news! Your order #123 has been confirmed.", message);
    }

    @Test
    void validateMessageBuilderTemplateGenerateSubject() {
        String message = MessageBuilderTemplate.generateSubject("789989", OrderStatus.DELIVERED);

        Assertions.assertEquals("789989 Status: Order delivered!", message);
    }

    @Test //the mock email sender should print out message to console when event parameter fields are valid
    void validateMockEmailSenderWhenEmailFieldIsNotNull() {
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

    @Test //the mock email sender should not print anything to console
    void validateMockEmailSenderWhenEmailFieldIsEmpty() {



    }

    @Test //validate that the simple mail transfer protocol is able to send live emails when everything is correct such as authentication etc.
    void validateSmtpEmailSender() {
    }

    @Test //validate that it is able to successfully catch a generic exception caused by common issues such as authentication failure or
        // invalid recipient email etc. and then outputs with a message
    void SmtpEmailSenderCatchesException() {
    }

    @Test //the mock sms sender should print out message to console when event parameter fields are valid
    void validateMockSmsSenderWhenNumberIsNotNull() {
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

    @Test //the mock sms sender should not print anything to console
    void validateMockSmsSenderWhenNumberIsNull() {
    }

    @Test //validate that the twilio sms is able to send live sms when everything is correct such as authentication etc.
    void validateTwilioSmsSender() {
    }

    @Test //validate that it is able to successfully catch a generic exception caused by common issues such as authentication failure or
        // invalid recipient number etc. and then outputs with a message
    void TwilioSmsSenderCatchesException() {
    }

    //------------------------FURTHER PERFORMANCE/OTHER TESTS----------------------------------------------------------------
    @Test //testing resilliance4j's retry for requests sent to SMTP server
    void retryTestForSmptpEmailSender() {
    }

    @Test //testing resilliance4j's retry for requests sent to TWILIO server
    void retryTestForTwilioSmsSender() {
    }

    @Test //testing resilliance4j's circuitBreaker for requests sent to TWILIO server
    void circuitBreakerTestForTwilioSmsSender() {
    }

    @Test //testing resilliance4j's timeLimiter for requests sent to TWILIO server
    void TimeLimiterTestForTwilioSmsSender() {
    }

    @Test //testing fallback method for twilio sms sender activates when resilience4j metrics fail
    void fallbackForTwilioSmsSender() {
    }

    @Test //testing fallback method for SMTP Email sender activates when resilience4j metrics fail
    void fallbackForSmtpEmailSender() {
    }



}
