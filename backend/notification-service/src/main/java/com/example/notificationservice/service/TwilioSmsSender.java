package com.example.notificationservice.service;

import com.example.notificationservice.customException.FailedToSendSmsException;
import com.twilio.Twilio;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component

public class TwilioSmsSender implements SmsSender {
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public TwilioSmsSender(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.from-number}") String fromNumber
    ) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        //log in to Twilio so future API calls are authenticated
        Twilio.init(accountSid, authToken);//initialize only once in constructor
        //more efficient and staches authtoekn/credentials to every request we make after

    }

    @Override
    public void sendSms(String recipientNumber, String message) {
        try {
            Message.creator(new PhoneNumber(recipientNumber), new PhoneNumber(fromNumber), message).create();
            //Twilio provided class and method to build message and send to twilio api
            // builds an HTTP POST request
            // sends it to Twilio servers
            // waits for response
            // returns a Message object
        }catch (TwilioException e) {
            throw new FailedToSendSmsException("Error Encountered! Failed To Send Email to: "+recipientNumber+", "+e.getMessage());
        }
    }
}
