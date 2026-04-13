package com.example.notificationservice;


import com.example.notificationservice.controller.NotificationController;
import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.event.OrderStatus;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.model.SentStatus;
import com.example.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import  org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
public class NotificationServiceEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
        //overall endpoint test, when request is received by our endpoint it should return a response back, wahterver it may be, this verifes iuts function as a rest api
    void validateEndpointReceiveOrderEvent() throws Exception {

        String json = """
        {
            "orderId": "2134",
            "email": "yoga@gmail.com",
            "phone": "09868795",
            "status": "DELIVERED"
        }
    """;

        // Mock service response
        Notification notification = new Notification();
        notification.setEmailSentStatus(SentStatus.SENT);
        notification.setSmsSentStatus(SentStatus.SENT);
        //since our service layer is mocked we need it to take in mocked parameters and return a real notification object we defined above
        //the reason this is , is because my controller layer logic will not send back 'ok' response unless sentStatus fields are not null!
        Mockito.when(notificationService.processOrderEvent(Mockito.any(OrderEventDTO.class))).thenReturn(notification);

        //once our mock service layer returns a real notification object m the status fields deicde the response ,
        // in this case since both are sent , it should be 'ok' response!
        //here we test the endpoints functionality , we tell it we want an 'ok' response
        mockMvc.perform(post("/notifications/order-event")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk());


        Mockito.verify(notificationService, Mockito.times(1))//an extra layer of verification to verify that the service
                .processOrderEvent(Mockito.any(OrderEventDTO.class));// layer method was indeed invoked
    }

    @Test //if our request is valid then a 'successful' response should be returned
    void validateEndpointSendSuccessfulResponse() throws Exception {

        String json = """
        {
            "orderId": "123",
            "email": "test@gmail.com",
            "phone": "123456789",
            "status": "DELIVERED"
        }
    """;

        // Mock service response
        Notification notification = new Notification();
        notification.setEmailSentStatus(SentStatus.SENT);
        notification.setSmsSentStatus(SentStatus.SENT);

        Mockito.when(notificationService.processOrderEvent(Mockito.any(OrderEventDTO.class))).thenReturn(notification);

        mockMvc.perform(post("/notifications/order-event").contentType("application/json").content(json)).andExpect(status().isOk());

    }

    @Test
    void shouldReturnPartialFailure_whenEmailFails() throws Exception {

        String json = """
        {
            "orderId": "123",
            "email": "test@gmail.com",
            "phone": "123456789",
            "status": "DELIVERED"
        }
    """;

        Notification notification = new Notification();
        notification.setEmailSentStatus(SentStatus.FAILED);
        notification.setSmsSentStatus(SentStatus.SENT);

        Mockito.when(notificationService.processOrderEvent(Mockito.any()))
                .thenReturn(notification);

        mockMvc.perform(post("/notifications/order-event")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnPartialFailure_whenSmsFails() throws Exception {

        String json = """
        {
            "orderId": "123",
            "email": "test@gmail.com",
            "phone": "123456789",
            "status": "DELIVERED"
        }
    """;

        Notification notification = new Notification();
        notification.setEmailSentStatus(SentStatus.SENT);
        notification.setSmsSentStatus(SentStatus.FAILED);

        Mockito.when(notificationService.processOrderEvent(Mockito.any()))
                .thenReturn(notification);

        mockMvc.perform(post("/notifications/order-event")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test //if our request is invalid as both email/sms failed to send then an 'unsuccessful' response should be returned, (It's still an http 'ok' response!
        // just the response should detail the failure!)
    void validateEndpointSendUnsuccessfulResponse() throws Exception {

        String json = """
        {
            "orderId": "123",
            "email": "test@gmail.com",
            "phone": "123456789",
            "status": "DELIVERED"
        }
    """;

        // Mock service response
        Notification notification = new Notification();
        notification.setEmailSentStatus(SentStatus.FAILED);
        notification.setSmsSentStatus(SentStatus.FAILED);

        //since our service layer is mocked we need it to take in mocked parameters and return a real notification object we defined above
        //the reason this is , is because my controller layer logic will not send back 'ok' response unless sentStatus fields are not null!
        Mockito.when(notificationService.processOrderEvent(Mockito.any(OrderEventDTO.class))).thenReturn(notification);

        //here we test the endpoints functionality , we tell it we want an 'ok' response , however within the 'ok' response it should contain the error message
        mockMvc.perform(post("/notifications/order-event").contentType("application/json").content(json)).andExpect(status().isOk())
                .andExpect(jsonPath("$.message",containsString("Order event processing: Failed! Unable To Send SMS To Recipient Phone Number, Unable To Send Email To Recipient Email! Please Check Both Parameters And Try Again!")));

    }


    @Test
    void shouldReturnBadRequest_whenInvalidInput() throws Exception {

        String json = """
        {
            "orderId": "",
            "email": "",
            "phone": "",
            "status": "DELIVERED"
        }
    """;

        mockMvc.perform(post("/notifications/order-event")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
