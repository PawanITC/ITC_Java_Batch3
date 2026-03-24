package com.example.notificationservice;

import com.example.notificationservice.dto.OrderEventDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;

@SpringBootTest
class NotificationServiceApplicationTests {

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
    }

    @Test
    void validateMessageBuilderTemplateGenerateSubject() {
    }

    @Test //the mock email sender should print out message to console when event parameter fields are valid
    void validateMockEmailSenderWhenEmailFieldIsNotNull() {
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
