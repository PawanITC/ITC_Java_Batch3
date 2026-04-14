package com.example.notificationservice.controller;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.model.SentStatus;
import com.example.notificationservice.response.ApiResponse;
import com.example.notificationservice.service.NotificationService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {//dependency injection for spring
        this.notificationService = notificationService;
    }


    @PostMapping("/order-event")
    public ResponseEntity<ApiResponse<Notification>> receiveOrderEvent(@RequestBody @Valid/*input validation*/ OrderEventDTO event) {

        Notification notification = notificationService.processOrderEvent(event);//if service layer processes everything without problems

        if (notification.getEmailSentStatus() == SentStatus.SENT && notification.getSmsSentStatus() == SentStatus.SENT) {
            return ResponseEntity.ok(new ApiResponse<>(notification,"Order event processed successfully!"));
        }//is true
        else if(notification.getEmailSentStatus() == SentStatus.FAILED && notification.getSmsSentStatus() == SentStatus.SENT){
            return ResponseEntity.ok(new ApiResponse<>(notification,"Order event processing: Partially failed! Unable To Send Email To Recipient, Please Check Recipient Email and Try Again!"));//if not
        } else if (notification.getEmailSentStatus() == SentStatus.SENT && notification.getSmsSentStatus() == SentStatus.FAILED) {
            return ResponseEntity.ok(new ApiResponse<>(notification,"Order event processing: Partially failed! Unable To Send SMS To Recipient Phone Number, Please Check Recipient Phone Number and Try Again!"));
        } else if (notification.getEmailSentStatus() == SentStatus.FAILED && notification.getSmsSentStatus() == SentStatus.FAILED) {
            return ResponseEntity.ok(new ApiResponse<>(notification,"Order event processing: Failed! Unable To Send SMS To Recipient Phone Number, Unable To Send Email To Recipient Email! Please Check Both Parameters And Try Again!"));
        }else return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(notification,"Order event processing failed! An Unexpected error occurred!"));

    }
}
//