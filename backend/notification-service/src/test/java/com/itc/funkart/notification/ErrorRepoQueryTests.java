package com.itc.funkart.notification;

import com.itc.funkart.notification.dto.OrderEventDTO;
import com.itc.funkart.notification.event.OrderStatus;
import com.itc.funkart.notification.model.NotificationErrorMessages;
import com.itc.funkart.notification.repository.NotificationErrorRepository;
import com.itc.funkart.notification.service.ErrorRepoQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
public class ErrorRepoQueryTests {

    OrderEventDTO orderEventDTO = new OrderEventDTO();

    @Autowired
    private ErrorRepoQuery errorRepoQuery;

    @MockBean
    private NotificationErrorRepository notificationErrorRepository;

    @BeforeEach
    void setup() {
        orderEventDTO.setOrderId("123");
        orderEventDTO.setEmail("noreply@gmail.com");
        orderEventDTO.setPhone("123456789");
        orderEventDTO.setStatus(OrderStatus.ORDER_CANCELLED);
    }

    @Test
    public void testUpdateEmailErrorRecordWhenRecordDoesNotExist() {
        Mockito.when(notificationErrorRepository.existsByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any())).thenReturn(false);

        errorRepoQuery.updateEmailErrorRecord(orderEventDTO, new Exception("E-mail failed!"));

        Mockito.verify(notificationErrorRepository, Mockito.times(1)).save(Mockito.argThat(record ->
                "E-mail failed!".equals(record.getEmailErrorMessage()) &&
                        record.getSmsErrorMessage() == null
        ));
        Mockito.verify(notificationErrorRepository, Mockito.never())
                .findByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testUpdateEmailErrorRecordWhenRecordExists() {
        Mockito.when(notificationErrorRepository.existsByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any())).thenReturn(true);

        NotificationErrorMessages existing = new NotificationErrorMessages("1", null, null, OrderStatus.ORDER_CONFIRMED);
        Mockito.when(notificationErrorRepository.findByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.of(existing));

        errorRepoQuery.updateEmailErrorRecord(orderEventDTO, new Exception("E-mail failed!"));

        Mockito.verify(notificationErrorRepository, Mockito.times(1))
                .findByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any());
        Mockito.verify(notificationErrorRepository, Mockito.times(1)).save(existing);
        Assertions.assertEquals("E-mail failed!", existing.getEmailErrorMessage());
        Assertions.assertEquals(OrderStatus.ORDER_CONFIRMED, existing.getOrderStatus());
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
        Mockito.when(notificationErrorRepository.findByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.of(existing));

        errorRepoQuery.updateSmsErrorRecord(orderEventDTO, new Exception("SMS failed!"));

        Mockito.verify(notificationErrorRepository, Mockito.times(1))
                .findByOrderIdAndOrderStatus(Mockito.anyString(), Mockito.any());
        Mockito.verify(notificationErrorRepository, Mockito.times(1)).save(existing);
        Assertions.assertEquals("SMS failed!", existing.getSmsErrorMessage());
        Assertions.assertEquals(OrderStatus.ORDER_CONFIRMED, existing.getOrderStatus());
    }
}
