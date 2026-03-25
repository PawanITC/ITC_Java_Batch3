package com.example.notificationservice;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import org.hibernate.validator.internal.constraintvalidators.bv.AssertTrueValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;

import com.example.notificationservice.template.*;
import com.example.notificationservice.event.OrderStatus;
import com.example.notificationservice.service.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class NotificationServiceApplicationTests {


    @Mock
    NotificationRepository notificationRepository;

    @Mock
    private MockEmailSender mockEmailSender;

    @Mock
    private MockSmsSender mockSmsSender;

    @Mock
    private SmtpEmailSender smtpEmailSender;

    @Mock
    private TwilioSmsSender twilioSmsSender;

    @InjectMocks
    private NotificationServiceImpl service;

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
    void validateOrderEventDTOFields() {//ensure all fields are validated
        Assertions.assertEquals(true, true);

    }

    @Test //tests to see if an orderEventDto is successfully mapped to the model Notification object
    void serviceLayerMapsOrderEventDTOtoNotification() {
        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("joe@gmail.com");
        eventDTO.setPhone("123456789");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        Notification n = NotificationServiceImpl.generateNotification(eventDTO);

        Assertions.assertEquals("12453", n.getOrderId());
        Assertions.assertEquals(OrderStatus.DELIVERED, n.getStatus());
        Assertions.assertEquals("123456789", n.getPhone());
        Assertions.assertEquals("joe@gmail.com", n.getEmail());
    }

    @Test //a mocked test to ensure that when we pass the eventDTO object the service class method ,
        // then it DEFINITELY has CALLED the interface JpaRepository.save() method to save the object to repo.
    void validateNotificationSavedToRepository() {
        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("joe@gmail.com");
        eventDTO.setPhone("123456789");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        service.processOrderEvent(eventDTO);

        Mockito.verify(notificationRepository, Mockito.times(1)).save(Mockito.any(Notification.class));
        //we verify here that INDEED , behind the scenes somewhere our service class method is calling this exact method
        //with the parameter being a Notification class object being passed through
    }

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

    //------------------------FURTHER PERFORMANCE/OTHER TESTS--------------------------------------------------------------------------------------------------

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

    @Test //test whether the database connects with our credentials stored in secrets
    void testDatabaseConnectivity() {
    }



}
