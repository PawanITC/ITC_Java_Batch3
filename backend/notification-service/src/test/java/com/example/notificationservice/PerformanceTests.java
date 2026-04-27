package com.example.notificationservice;

import org.junit.jupiter.api.Test;

public class PerformanceTests {

    //------------------------FURTHER PERFORMANCE/OTHER TESTS--------------------------------------------------------------------------------------------------

    @Test
        //testing resilliance4j's retry for requests sent to SMTP server
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
