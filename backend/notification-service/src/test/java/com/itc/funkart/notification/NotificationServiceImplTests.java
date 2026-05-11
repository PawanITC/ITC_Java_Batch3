package com.itc.funkart.notification;

import com.itc.funkart.notification.dto.OrderEventDTO;
import com.itc.funkart.notification.event.OrderStatus;
import com.itc.funkart.notification.exception.FailedToSendEmailException;
import com.itc.funkart.notification.exception.FailedToSendSmsException;
import com.itc.funkart.notification.model.Notification;
import com.itc.funkart.notification.repository.NotificationRepository;
import com.itc.funkart.notification.service.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest
@ActiveProfiles("test")
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

    @Autowired
    private NotificationServiceImpl service;

    @Test
    void sanityCheckInjection() {
        assertSame(notificationRepository, ReflectionTestUtils.getField(service, "repository"));
        assertSame(twilioSmsSender, ReflectionTestUtils.getField(service, "twilioSmsSender"));
        assertSame(smtpEmailSender, ReflectionTestUtils.getField(service, "smtpEmailSender"));
        assertSame(mockSmsSender, ReflectionTestUtils.getField(service, "mockSmsSender"));
        assertSame(mockEmailSender, ReflectionTestUtils.getField(service, "mockEmailSender"));
    }

    @Test
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

    @Test
    void validateNotificationSavedToRepository() {
        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("joe@gmail.com");
        eventDTO.setPhone("123456789");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        service.processOrderEvent(eventDTO);

        Mockito.verify(notificationRepository, Mockito.times(1)).save(Mockito.any(Notification.class));
    }

    @Test
    void validateEmailSenderWhenEmailFieldIsEmpty() {
        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("");
        eventDTO.setPhone("123456789");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        service.processOrderEvent(eventDTO);

        Mockito.verify(smtpEmailSender, Mockito.never()).sendEmail(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(mockEmailSender, Mockito.never()).sendEmail(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void validateSmtpEmailSender() {
        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("Joe@gmail.com");
        eventDTO.setPhone("123456789");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        service.processOrderEvent(eventDTO);

        Mockito.verify(smtpEmailSender, Mockito.times(1)).sendEmail(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
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

        Mockito.verify(errorRepoQuery, Mockito.times(1))
                .updateEmailErrorRecord(Mockito.eq(eventDTO), Mockito.any(FailedToSendEmailException.class));
    }

    @Test
    void validateMockSmsSenderWhenNumberIsNull() {
        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("Joe@hotmail.com");
        eventDTO.setPhone("");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        service.processOrderEvent(eventDTO);

        Mockito.verify(mockSmsSender, Mockito.never()).sendSms(Mockito.any(), Mockito.any());
        Mockito.verify(twilioSmsSender, Mockito.never()).sendSms(Mockito.any(), Mockito.any());
    }

    @Test
    void validateTwilioSmsSender() {
        OrderEventDTO eventDTO = new OrderEventDTO();
        eventDTO.setOrderId("12453");
        eventDTO.setEmail("Joe@hotmail.com");
        eventDTO.setPhone("07871976543");
        eventDTO.setStatus(OrderStatus.DELIVERED);

        service.processOrderEvent(eventDTO);

        Mockito.verify(twilioSmsSender).sendSms(Mockito.any(String.class), Mockito.any(String.class));
    }

    @Test
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

        Mockito.verify(errorRepoQuery, Mockito.times(1))
                .updateSmsErrorRecord(Mockito.eq(eventDTO), Mockito.any(FailedToSendSmsException.class));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        }
    }
}
