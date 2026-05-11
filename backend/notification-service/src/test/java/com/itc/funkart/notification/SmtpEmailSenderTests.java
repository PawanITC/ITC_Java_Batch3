package com.itc.funkart.notification;

import com.itc.funkart.notification.exception.FailedToSendEmailException;
import com.itc.funkart.notification.service.SmtpEmailSender;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {"management.health.mail.enabled=false"})
@ActiveProfiles("test")
public class SmtpEmailSenderTests {

    @Autowired
    private SmtpEmailSender smtpEmailSender;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    public void sendEmailTest() {
        smtpEmailSender.sendEmail("idontknow@gmail.com", "holiday", "hey wassup hows the holiday");
        Mockito.verify(javaMailSender).send(Mockito.any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_shouldThrowCustomException_whenMailFails() {
        Mockito.doThrow(new MailSendException("SMTP error"))
                .when(javaMailSender)
                .send(Mockito.any(SimpleMailMessage.class));

        assertThrows(FailedToSendEmailException.class, () ->
                smtpEmailSender.sendEmail("test@gmail.com", "subject", "message")
        );
    }
}
