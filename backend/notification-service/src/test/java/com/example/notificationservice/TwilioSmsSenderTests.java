package com.example.notificationservice;

import com.example.notificationservice.exception.FailedToSendSmsException;
import com.example.notificationservice.service.TwilioSmsSender;
import com.twilio.exception.ApiException;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;

import com.twilio.rest.api.v2010.account.MessageCreator;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import com.twilio.Twilio;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import io.micrometer.observation.annotation.Observed;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "twilio.account-sid=testSid",
        "twilio.auth-token=testToken",
        "twilio.from-number=+123456789"
})
public class TwilioSmsSenderTests {

    @Autowired
    private TwilioSmsSender smsSender;

    @Test
    void sendSms_success() {

        try (MockedStatic<Message> mockedMessage = Mockito.mockStatic(Message.class)) {

            MessageCreator creatorMock = Mockito.mock(MessageCreator.class);

            mockedMessage.when(() ->
                    Message.creator(
                            Mockito.any(PhoneNumber.class),
                            Mockito.any(PhoneNumber.class),
                            Mockito.anyString()
                    )
            ).thenReturn(creatorMock);

            Mockito.when(creatorMock.create()).thenReturn(Mockito.mock(Message.class));

            // Act (no exception expected)
            smsSender.sendSms("+447123456789", "hello");
        }
    }

    @Test
    void sendSms_shouldThrowCustomException_whenTwilioFails() {

        try (MockedStatic<Message> mockedMessage = Mockito.mockStatic(Message.class)) {

            MessageCreator creatorMock = Mockito.mock(MessageCreator.class);

            mockedMessage.when(() ->
                    Message.creator(
                            Mockito.any(PhoneNumber.class),
                            Mockito.any(PhoneNumber.class),
                            Mockito.anyString()
                    )
            ).thenReturn(creatorMock);

            Mockito.when(creatorMock.create())
                    .thenThrow(new ApiException("Twilio error"));

            // Assert
            assertThrows(FailedToSendSmsException.class, () -> {
                smsSender.sendSms("+447123456789", "hello");
            });
        }
    }

}
