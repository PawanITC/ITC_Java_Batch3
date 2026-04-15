package com.example.notificationservice;

import com.example.notificationservice.exception.FailedToSendEmailException;
import com.example.notificationservice.exception.FailedToSendSmsException;
import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;

import com.example.notificationservice.event.OrderStatus;
import com.example.notificationservice.service.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
//@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTests {


    @MockBean
    NotificationRepository notificationRepository;

    @MockBean
    private MockEmailSender mockEmailSender;

    @MockBean
    private MockSmsSender mockSmsSender;

    @MockBean
    private SmtpEmailSender smtpEmailSender;

    @MockBean
    private TwilioSmsSender twilioSmsSender;

    @MockBean
    private ErrorRepoQuery errorRepoQuery;

    @TestConfiguration
    static class TestConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        }
    }
//    @MockBean
//    private Counter emailSentCounter;
//    @MockBean
//    private Counter emailFailedCounter;
//    @MockBean
//    private Counter smsSentCounter;
//    @MockBean
//    private Counter smsFailedCounter;
//    @MockBean
//    private MeterRegistry meterRegistry;

    @Autowired
    private NotificationServiceImpl service;


    @Test
    void contextLoads() {
    }

    @Test
    void sanityCheckInjection() {
        assertSame(notificationRepository, ReflectionTestUtils.getField(service, "repository"));
        assertSame(twilioSmsSender, ReflectionTestUtils.getField(service, "twilioSmsSender"));
        assertSame(smtpEmailSender, ReflectionTestUtils.getField(service, "smtpEmailSender"));
        assertSame(mockSmsSender, ReflectionTestUtils.getField(service, "mockSmsSender"));
        assertSame(mockEmailSender, ReflectionTestUtils.getField(service, "mockEmailSender"));
    }

    //------------------------MAIN FUNCTIONAL TESTS-------------------------------------------------------------------------

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

    @Test //testing both the mock email sender and live smtp email senders. i.e they should not initiate trying to send email because e-mail field is empty
    void validateEmailSenderWhenEmailFieldIsEmpty() {

        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("");
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
        // invalid recipient email etc. and then adds the error details to the error repository
    void SmtpEmailSenderCatchesException() {
        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("Joe@gmail.com");
        eventDTO.setPhone("123456789");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        Mockito.doThrow(new FailedToSendEmailException("error, unable to send email!"))
                .when(smtpEmailSender)
                .sendEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        service.processOrderEvent(eventDTO);

        //we verify that the errorrepositoy was updated to save error record
        Mockito.verify(errorRepoQuery, Mockito.times(1)).updateEmailErrorRecord(Mockito.eq(eventDTO),Mockito.any(FailedToSendEmailException.class));//over here mockito.never() tests to see if that specific was
        //was truly not called
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

        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("Joe@gmail.com");
        eventDTO.setPhone("123456789");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        Mockito.doThrow(new FailedToSendSmsException("error, unable to send sms!"))
                .when(twilioSmsSender)
                .sendSms(Mockito.anyString(), Mockito.anyString());

        service.processOrderEvent(eventDTO);

        //we verify that the errorrepositoy was updated to save error record
        Mockito.verify(errorRepoQuery, Mockito.times(1)).updateSmsErrorRecord(Mockito.eq(eventDTO),Mockito.any(FailedToSendSmsException.class));//over here mockito.never() tests to see if that specific was
        //was truly not called

    }


}
