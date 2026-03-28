package br.com.nicolas.user_service.user.producer;



import br.com.nicolas.user_service.user.dtos.UserResponseDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserProducer {
  private final RabbitTemplate rabbitTemplate;

  public UserProducer(RabbitTemplate rabbitTemplate){
    this.rabbitTemplate = rabbitTemplate;
  }

  public void userCreated(UserResponseDTO userResponseDTO){
    rabbitTemplate.convertAndSend(
            "user.exchange",
            "user.created",
            userResponseDTO
    );
  }



}
