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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import  org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
public class NotificationServiceEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
        //overall endpoint test, when request is received by our endpoint it should return a response back
    void validateEndpointReceiveOrderEvent() {

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

        mockMvc.perform(post("/notifications/order-event").contentType("application/json").content(json)).andExpect(MockMvcResultMatchers.status().isOk());

    }

    @Test //if our request is invalid then an 'unsuccessful' response should be returned
    void validateEndpointSendUnsuccessfulResponse() {

    }
}
