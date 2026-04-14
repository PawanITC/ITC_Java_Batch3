package com.example.notificationservice.service;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.model.NotificationErrorMessages;
import com.example.notificationservice.repository.NotificationErrorRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ErrorRepoQuery {

    public ErrorRepoQuery(NotificationErrorRepository notificationErrorRepository) {
        this.notificationErrorRepository = notificationErrorRepository;
    }

    private final NotificationErrorRepository notificationErrorRepository;
    @Observed(name = "update-Email-Error-Record")
    public void updateEmailErrorRecord(OrderEventDTO event, Exception e){
        Boolean exists = checkRecordExists(event);

        if(exists){

            Optional<NotificationErrorMessages> errorRecord = notificationErrorRepository.findByOrderIdAndOrderStatus(event.getOrderId(), event.getStatus());
            errorRecord.get().setEmailErrorMessage(e.getMessage());
            notificationErrorRepository.save(errorRecord.get());//update the missing parameter

        }else{//otherwise well create a new error record
            notificationErrorRepository.save(new NotificationErrorMessages(event.getOrderId(), e.getMessage(),null,event.getStatus()));
        }//
    }
    @Observed(name = "update-Sms-Error-Record")
    public void updateSmsErrorRecord(OrderEventDTO event, Exception e){
        Boolean exists = checkRecordExists(event);

        if(exists){
            //exists in the database, if so we'll just update the record .
            Optional<NotificationErrorMessages> errorRecord = notificationErrorRepository.findByOrderIdAndOrderStatus(event.getOrderId(), event.getStatus());
            errorRecord.get().setSmsErrorMessage(e.getMessage());
            notificationErrorRepository.save(errorRecord.get());//update the missing parameter
        }else {
            //otherwise well create a new error record
            notificationErrorRepository.save(new NotificationErrorMessages(event.getOrderId(), null,e.getMessage(), event.getStatus()));
        }
    }

    private boolean checkRecordExists(OrderEventDTO event){//we'll check first if the error record already
        //exists in the database, if so we'll just update the record .
        return notificationErrorRepository.existsByOrderIdAndOrderStatus(event.getOrderId(), event.getStatus()) ;

    }
}
