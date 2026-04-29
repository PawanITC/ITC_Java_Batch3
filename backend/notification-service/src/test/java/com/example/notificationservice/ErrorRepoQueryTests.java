package com.example.notificationservice;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.event.OrderStatus;
import com.example.notificationservice.model.NotificationErrorMessages;
import com.example.notificationservice.repository.NotificationErrorRepository;
import com.example.notificationservice.service.ErrorRepoQuery;
import com.example.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

@SpringBootTest
public class ErrorRepoQueryTests {
    @Autowired
    private ErrorRepoQuery errorRepoQuery;
    @MockBean
    private NotificationErrorRepository  notificationErrorRepository;

    OrderEventDTO orderEventDTO = new OrderEventDTO();

    @BeforeEach
    void setup(){//we need to set all the details as orderEventDTO has field validation that it won't let you skip
        orderEventDTO.setOrderId("123");
        orderEventDTO.setEmail("noreply@gmail.com");
        orderEventDTO.setPhone("123456789");
        orderEventDTO.setStatus(OrderStatus.ORDER_CANCELLED);
    }


    @Test//if a record does not already exist in the database then our class should create a new error record
    public void testUpdateEmailErrorRecordWhenRecordDoesNotExist() {
        Mockito.when(notificationErrorRepository.existsByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any())).thenReturn(false);

        errorRepoQuery.updateEmailErrorRecord(orderEventDTO, new Exception("E-mail failed!"));

        Mockito.verify(notificationErrorRepository, Mockito.times(1)).save(Mockito.argThat(record ->
                "E-mail failed!".equals(record.getEmailErrorMessage()) &&
                        record.getSmsErrorMessage() == null
        ));

        Mockito.verify(notificationErrorRepository, Mockito.never()).findByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any());//extra verification
        //just to be sure , that creating a new record logic did not accidentally invoke findbyorderid... which would mean it would lead to updating exiting record instead

    }

    @Test //if a record does exist in the database then our class should just update the existing error record
    public void testUpdateEmailErrorRecordWhenRecordExists() {
        Mockito.when(notificationErrorRepository.existsByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any())).thenReturn(true);//first we make sure our
        //mocked errorRepo object returns true, wherever it gets invoked within errorepo query class.

        //we make our error message object
        NotificationErrorMessages existing = new NotificationErrorMessages("1", null, null, OrderStatus.ORDER_CONFIRMED);
        //now when our mocked class tries to call find by order id.. since its only mocked it won't return anything, hence we won't be able to test our
        //scenario, so we force it to return our error message object as the 'record' found .
        Mockito.when(notificationErrorRepository.findByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(existing));

        errorRepoQuery.updateEmailErrorRecord(orderEventDTO, new Exception("E-mail failed!"));//no we run the actual class method and let the action unfold as we intended
        //(with returning objects when method is invoked)

        //and over here we can finally verify , whether the mocked object did indeed call these methods behind the scenes
        Mockito.verify(notificationErrorRepository, Mockito.times(1)).findByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any());
        Mockito.verify(notificationErrorRepository, Mockito.times(1)).save(existing);//since we know what record it was as we created it

        Assertions.assertEquals("E-mail failed!",existing.getEmailErrorMessage());//if our logic worked properly it should have updated our
        Assertions.assertEquals(OrderStatus.ORDER_CONFIRMED,existing.getOrderStatus());//'record' with the parameters we passed earlier
        //hence we can prove our test passes as all methods were invoked correctly with the correct parameters and the assertions were true
    }

    @Test
    public void testUpdateSmsErrorRecordWhenRecordDoesNotExist() {
        Mockito.when(notificationErrorRepository.existsByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any())).thenReturn(false);

        errorRepoQuery.updateSmsErrorRecord(orderEventDTO, new Exception("SMS failed!"));

        Mockito.verify(notificationErrorRepository, Mockito.times(1)).save(Mockito.argThat(record ->
                "SMS failed!".equals(record.getSmsErrorMessage()) &&
                        record.getEmailErrorMessage() == null
        ));

        Mockito.verify(notificationErrorRepository, Mockito.never())
                .findByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testUpdateSmsErrorRecordWhenRecordExists() {
        Mockito.when(notificationErrorRepository.existsByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any())).thenReturn(true);

        NotificationErrorMessages existing = new NotificationErrorMessages("1", null, null, OrderStatus.ORDER_CONFIRMED);
        Mockito.when(notificationErrorRepository.findByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(existing));

        errorRepoQuery.updateSmsErrorRecord(orderEventDTO, new Exception("SMS failed!"));

        Mockito.verify(notificationErrorRepository, Mockito.times(1)).findByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any());
        Mockito.verify(notificationErrorRepository, Mockito.times(1)).save(existing);

        Assertions.assertEquals("SMS failed!",existing.getSmsErrorMessage());//if our logic worked properly it should have updated our
        Assertions.assertEquals(OrderStatus.ORDER_CONFIRMED,existing.getOrderStatus());//'record' with the parameters we passed earlier
        //hence we can prove our test passes as all methods were invoked correctly with the correct parameters and the assertions were true
    }

}
