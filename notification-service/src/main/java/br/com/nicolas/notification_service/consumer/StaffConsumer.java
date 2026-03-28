package br.com.nicolas.notification_service.consumer;


import br.com.nicolas.notification_service.config.RabbitmqConfig;
import br.com.nicolas.notification_service.dtos.EmailDTO;
import br.com.nicolas.notification_service.service.EmailService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@Component
public class StaffConsumer {
  private static final Logger log = LoggerFactory.getLogger(StaffConsumer.class);
  private final EmailService emailService;
  private final RabbitTemplate rabbitTemplate;
  private final int maxAttempts = 4;

  public StaffConsumer(EmailService emailService, RabbitTemplate rabbitTemplate) {
    this.emailService = emailService;
    this.rabbitTemplate = rabbitTemplate;
  }

  @RabbitListener(queues = RabbitmqConfig.STAFF_QUEUE, containerFactory = "rabbitListenerContainerFactory")
  public void listenStaff(EmailDTO emailDTO, Message message, Channel channel) throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    try {
      log.info("Processando notificação para staff: {}", emailDTO.to());
      emailService.send(emailDTO);
      channel.basicAck(deliveryTag, false);
      log.info("Email enviado para staff: {}", emailDTO.to());
    } catch (Exception e) {
      int attempts = extractRetryCount(message);
      log.error("Falha ao enviar email para staff: {} - tentativa {} - erro: {}",
        emailDTO.to(), attempts + 1, e.getMessage());

      if (attempts + 1 >= maxAttempts) {
        log.warn("Máximo de tentativas alcançado para {}. Encaminhando para DLQ.", emailDTO.to());
        try {
          rabbitTemplate.convertAndSend(RabbitmqConfig.STAFF_DLX, RabbitmqConfig.STAFF_DLQ_ROUTING, emailDTO);
          channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
          log.error("Falha ao enviar para DLQ: {}", ex.getMessage(), ex);
          channel.basicAck(deliveryTag, false);
        }
      } else {
        channel.basicNack(deliveryTag, false, false);
        log.info("Mensagem enviada para retry. tentativa={}", attempts + 1);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private int extractRetryCount(Message message) {
    Map<String, Object> headers = message.getMessageProperties().getHeaders();
    Object xDeathObj = headers.get("x-death");
    if (xDeathObj instanceof List) {
      List<Map<String, Object>> xDeath = (List<Map<String, Object>>) xDeathObj;
      int totalCount = 0;
      for (Map<String, Object> death : xDeath) {
        Object count = death.get("count");
        if (count instanceof Long) totalCount += ((Long) count).intValue();
        else if (count instanceof Integer) totalCount += (Integer) count;
      }
      return totalCount;
    }
    return 0;
  }
}
