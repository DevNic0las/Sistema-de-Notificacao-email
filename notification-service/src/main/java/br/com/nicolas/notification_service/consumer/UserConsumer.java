package br.com.nicolas.notification_service.consumer;



import br.com.nicolas.notification_service.config.RabbitmqConfig;
import br.com.nicolas.notification_service.dtos.EmailDTO;
import br.com.nicolas.notification_service.service.EmailService;
import br.com.nicolas.notification_service.dtos.UserResponseDTO;
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
public class UserConsumer {
  private static final Logger log = LoggerFactory.getLogger(UserConsumer.class);
  private final EmailService emailService;
  private final RabbitTemplate rabbitTemplate;
  private final int maxAttempts = 4;

  public UserConsumer(EmailService emailService, RabbitTemplate rabbitTemplate) {
    this.emailService = emailService;
    this.rabbitTemplate = rabbitTemplate;

  }

  @RabbitListener(queues = RabbitmqConfig.USER_CREATED_QUEUE, containerFactory = "rabbitListenerContainerFactory")
  public void listenUserCreated(UserResponseDTO userResponseDTO, Message message, Channel channel) throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    try {
      log.info("Consumindo evento para usuário: {}", userResponseDTO.email());
      EmailDTO emailDTO = new EmailDTO(userResponseDTO.email(), "Bem vindo ao Sistema", "Olá "
        + userResponseDTO.email() + " sua conta foi criada com sucesso!");
      emailService.send(emailDTO);
      channel.basicAck(deliveryTag, false);
      log.info("E-mail enviado com sucesso para: {}", userResponseDTO.email());

    } catch (Exception e) {
      int attempts = extractRetryCount(message);
      log.error("Falha ao processar mensagem para {} - tentativa {} - erro: {}", userResponseDTO.email(), attempts + 1, e.getMessage());

      if (attempts + 1 >= maxAttempts) {

        try {
          log.warn("Máximo de tentativas alcançado para {}. Encaminhando para DLQ.", userResponseDTO.email());

          rabbitTemplate.convertAndSend(RabbitmqConfig.USER_DLX, RabbitmqConfig.USER_DLQ_ROUTING, userResponseDTO);
          channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
          log.error("Falha ao enviar mensagem para DLQ: {}", ex.getMessage(), ex);

          channel.basicAck(deliveryTag, false);
        }
      } else {
        channel.basicNack(deliveryTag, false, false);
        log.info("Mensagem enviada para fila de retry (broker-based). tentativa={}", attempts + 1);
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
        if (count instanceof Long) {
          totalCount += ((Long) count).intValue();
        } else if (count instanceof Integer) {
          totalCount += (Integer) count;
        }
      }
      return totalCount;
    }
    return 0;
  }
}
