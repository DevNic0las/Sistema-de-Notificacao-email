package br.com.nicolas.notification_service.service;


import br.com.nicolas.notification_service.consumer.StaffConsumer;
import br.com.nicolas.notification_service.dtos.EmailDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.MimeMessageHelper;

@Service
public class EmailService {
  private static final Logger log = LoggerFactory.getLogger(EmailService.class);
  private final JavaMailSender mailSender;
  private String emailSender = "teste@gmail.com";

  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void send(EmailDTO emailDTO) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(emailSender);
      helper.setTo(emailDTO.to());
      helper.setSubject(emailDTO.subject());
      helper.setText("<html><body><p>" + emailDTO.body() + "</p></body></html>", true);

      mailSender.send(message);
      log.info("E-mail enviado com sucesso para: {} ", emailDTO.to());
    } catch (MessagingException e) {
      log.error("Falha ao enviar e-mail: {} ", e.getMessage());
      throw new RuntimeException("Erro no envio de e-mail", e);
    }

  }
}
