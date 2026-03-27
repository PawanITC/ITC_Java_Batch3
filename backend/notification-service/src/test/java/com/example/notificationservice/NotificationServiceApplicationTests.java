package com.example.notificationservice;

import com.example.notificationservice.controller.NotificationController;
import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.response.ApiResponse;
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
import org.springframework.http.ResponseEntity;
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

    @InjectMocks
    NotificationController n ;

    @Mock
    private NotificationServiceImpl service2;

    @Test
    void contextLoads() {
    }

    void apiEndpointTests(){

    }

    //------------------------MAIN FUNCTIONAL TESTS-------------------------------------------------------------------------

    @Test //overall endpoint test, when request is received by our endpoint it should return a response back
    void validateEndpointReceiveOrderEvent() {

    }

    @Test //if our request is valid then a 'successful' response should be returned
    void validateEndpointSendSuccessfulResponse() {
        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("joe@gmail.com");
        eventDTO.setPhone("123456789");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        n.receiveOrderEvent(eventDTO);

        //TODO complete this test case
    }

    @Test //if our request is invalid then an 'unsuccessful' response should be returned
    void validateEndpointSendUnsuccessfulResponse() {

    }

    @Test //ensure we receive event dto with correctly validated fields
    void validateOrderEventDTOFields() {//ensure all fields are validated


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

    @Test //testing both the mock email sender and live smtp email senders. i.e they should not initiate trying to send email because e-mail field is empty
    void validateEmailSenderWhenEmailFieldIsEmpty() {

        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("  ");
        eventDTO.setPhone("123456789");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        service.processOrderEvent(eventDTO);

        Mockito.verify(smtpEmailSender, Mockito.never()).sendEmail(Mockito.any(),Mockito.any(),Mockito.any());//over here mockito.never() tests to see if that specific was
        //was truly not called
        Mockito.verify(mockEmailSender, Mockito.never()).sendEmail(Mockito.any(),Mockito.any(),Mockito.any());

    }

    @Test //validate that the simple mail transfer protocol is able to send live emails when everything is correct such as authentication etc.
    void validateSmtpEmailSender() {

        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("Joe@gmail.com");
        eventDTO.setPhone("123456789");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        service.processOrderEvent(eventDTO);

        Mockito.verify(smtpEmailSender, Mockito.times(1)).sendEmail(Mockito.any(),Mockito.any(),Mockito.any());//over here mockito.never() tests to see if that specific was
        //was truly not called
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

        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("Joe@hotmail.com");
        eventDTO.setPhone("");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        service.processOrderEvent(eventDTO);//real service , but uses mocked field parameter (mocksmssender, twilio etc.)

        Mockito.verify(mockSmsSender, Mockito.never()).sendSms(Mockito.any(),Mockito.any());//over here mockito.never() tests to see if that specific was
        //was truly not called
        Mockito.verify(twilioSmsSender, Mockito.never()).sendSms(Mockito.any(),Mockito.any());
    }

    @Test //validate that the twilio sms is able to send live sms when everything is correct such as authentication etc.
    void validateTwilioSmsSender() {

        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("Joe@hotmail.com");
        eventDTO.setPhone("07871976543");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        service.processOrderEvent(eventDTO);//real service , but uses mocked field parameter (mocksmssender, twilio etc.)

        Mockito.verify(twilioSmsSender).sendSms(Mockito.any(String.class),Mockito.any(String.class));
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
