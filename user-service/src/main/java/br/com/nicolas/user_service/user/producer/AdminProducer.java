package br.com.nicolas.user_service.user.producer;



import br.com.nicolas.user_service.user.dtos.EmailDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminProducer {
  private final RabbitTemplate rabbitTemplate;

  public AdminProducer(RabbitTemplate rabbitTemplate){
    this.rabbitTemplate = rabbitTemplate;
  }
  public  void sendToStaff(EmailDTO emailDTO){
    rabbitTemplate.convertAndSend("staff.exchange","staff.announcement.key", emailDTO);
  }


}
