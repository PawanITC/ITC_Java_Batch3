package com.example.notificationservice;

import com.example.notificationservice.exception.FailedToSendEmailException;
import com.example.notificationservice.service.SmtpEmailSender;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "management.health.mail.enabled=false"
})//we need this config set to false otherwise mail health does not allow mocked javamailsender
public class SmtpEmailSenderTests {
    @Autowired
    private SmtpEmailSender smtpEmailSender;
    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    public void sendEmailTest(){

        smtpEmailSender.sendEmail("idontknow@gmail.com","holiday","hey wassup hows the holiday");
        Mockito.verify(javaMailSender).send(Mockito.any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_shouldThrowCustomException_whenMailFails() {

        // Arrange: force mailSender to throw exception
        Mockito.doThrow(new MailSendException("SMTP error"))
                .when(javaMailSender)
                .send(Mockito.any(SimpleMailMessage.class));

        // Act + Assert
       assertThrows(FailedToSendEmailException.class, () -> {
            smtpEmailSender.sendEmail("test@gmail.com", "subject", "message");
       });
    }

}
