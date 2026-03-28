package br.com.nicolas.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class RabbitmqConfig {

  public static final String USER_CREATED_QUEUE = "user.email.queue";
  public static final String EXCHANGE_USER = "user.exchange";
  public static final String ROUTING_KEY_USER = "user.created";

  public static final String STAFF_QUEUE = "staff.queue";
  public static final String EXCHANGE_STAFF = "staff.exchange";
  public static final String ROUTING_KEY_STAFF = "staff.announcement.key";

  public static final String USER_DLX = "user.dlx";
  public static final String USER_DLQ = "user.email.dlq";
  public static final String USER_DLQ_ROUTING = "user.dead";

  public static final String STAFF_DLX = "staff.dlx";
  public static final String STAFF_DLQ = "staff.email.dlq";
  public static final String STAFF_DLQ_ROUTING = "staff.dead";

  public static final String USER_RETRY_EXCHANGE = "user.retry.exchange";
  public static final String USER_RETRY_QUEUE_1 = "user.email.retry.1";
  public static final String USER_RETRY_QUEUE_2 = "user.email.retry.2";
  public static final String USER_RETRY_QUEUE_3 = "user.email.retry.3";

  public static final String STAFF_RETRY_EXCHANGE = "staff.retry.exchange";
  public static final String STAFF_RETRY_QUEUE_1 = "staff.email.retry.1";
  public static final String STAFF_RETRY_QUEUE_2 = "staff.email.retry.2";
  public static final String STAFF_RETRY_QUEUE_3 = "staff.email.retry.3";

  public static final String USER_RETRY_ROUTING_1 = "user.retry.1";
  public static final String USER_RETRY_ROUTING_2 = "user.retry.2";
  public static final String USER_RETRY_ROUTING_3 = "user.retry.3";

  public static final String STAFF_RETRY_ROUTING_1 = "staff.retry.1";
  public static final String STAFF_RETRY_ROUTING_2 = "staff.retry.2";
  public static final String STAFF_RETRY_ROUTING_3 = "staff.retry.3";

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(jsonMessageConverter());

    template.setMandatory(true);
    return template;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jsonMessageConverter());

    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

    factory.setConcurrentConsumers(3);
    factory.setMaxConcurrentConsumers(10);
    factory.setPrefetchCount(10);
    return factory;
  }

  @Bean
  public Queue adminQueue() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", STAFF_RETRY_EXCHANGE);
    args.put("x-dead-letter-routing-key", STAFF_RETRY_ROUTING_1);
    return new Queue(STAFF_QUEUE, true, false, false, args);
  }

  @Bean
  public TopicExchange adminExchange() {
    return new TopicExchange(EXCHANGE_STAFF);
  }

  @Bean
  public Binding bindingAdminToStaff() {
    return BindingBuilder
      .bind(adminQueue())
      .to(adminExchange())
      .with(ROUTING_KEY_STAFF);
  }

  @Bean
  public Queue userCreatedQueue() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", USER_RETRY_EXCHANGE);

    args.put("x-dead-letter-routing-key", USER_RETRY_ROUTING_1);
    return new Queue(USER_CREATED_QUEUE, true, false, false, args);
  }

  @Bean
  public TopicExchange exchangeUserCreated() {
    return new TopicExchange(EXCHANGE_USER);
  }

  @Bean
  public Binding bindingUserCreated() {
    return BindingBuilder
      .bind(userCreatedQueue())
      .to(exchangeUserCreated())
      .with(ROUTING_KEY_USER);
  }

  @Bean
  public TopicExchange userRetryExchange() {
    return new TopicExchange(USER_RETRY_EXCHANGE);
  }

  @Bean
  public TopicExchange staffRetryExchange() {
    return new TopicExchange(STAFF_RETRY_EXCHANGE);
  }

  @Bean
  public Queue userRetryQueue1() {
    Map<String, Object> args = new HashMap<>();
    // after TTL, dead-letter back to main exchange with original routing key
    args.put("x-dead-letter-exchange", EXCHANGE_USER);
    args.put("x-dead-letter-routing-key", ROUTING_KEY_USER);

    args.put("x-message-ttl", 5000);
    return new Queue(USER_RETRY_QUEUE_1, true, false, false, args);
  }

  @Bean
  public Queue staffRetryQueue1() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", EXCHANGE_STAFF);
    args.put("x-dead-letter-routing-key", ROUTING_KEY_STAFF);
    args.put("x-message-ttl", 5000);
    return new Queue(STAFF_RETRY_QUEUE_1, true, false, false, args);
  }

  @Bean
  public Binding bindingRetry1() {
    return BindingBuilder.bind(userRetryQueue1()).to(userRetryExchange()).with(USER_RETRY_ROUTING_1);
  }

  @Bean
  public Binding bindingStaffRetry1() {
    return BindingBuilder.bind(staffRetryQueue1()).to(staffRetryExchange()).with(STAFF_RETRY_ROUTING_1);
  }

  @Bean
  public Queue userRetryQueue2() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", EXCHANGE_USER);
    args.put("x-dead-letter-routing-key", ROUTING_KEY_USER);
    args.put("x-message-ttl", 20000);
    return new Queue(USER_RETRY_QUEUE_2, true, false, false, args);
  }

  @Bean
  public Queue staffRetryQueue2() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", EXCHANGE_STAFF);
    args.put("x-dead-letter-routing-key", ROUTING_KEY_STAFF);
    args.put("x-message-ttl", 20000);
    return new Queue(STAFF_RETRY_QUEUE_2, true, false, false, args);
  }

  @Bean
  public Binding bindingRetry2() {
    return BindingBuilder.bind(userRetryQueue2()).to(userRetryExchange()).with(USER_RETRY_ROUTING_2);
  }

  @Bean
  public Binding bindingStaffRetry2() {
    return BindingBuilder.bind(staffRetryQueue2()).to(staffRetryExchange()).with(STAFF_RETRY_ROUTING_2);
  }

  @Bean
  public Queue userRetryQueue3() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", EXCHANGE_USER);
    args.put("x-dead-letter-routing-key", ROUTING_KEY_USER);
    args.put("x-message-ttl", 60000);
    return new Queue(USER_RETRY_QUEUE_3, true, false, false, args);
  }

  @Bean
  public Queue staffRetryQueue3() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", EXCHANGE_STAFF);
    args.put("x-dead-letter-routing-key", ROUTING_KEY_STAFF);
    args.put("x-message-ttl", 60000);
    return new Queue(STAFF_RETRY_QUEUE_3, true, false, false, args);
  }

  @Bean
  public Binding bindingRetry3() {
    return BindingBuilder.bind(userRetryQueue3()).to(userRetryExchange()).with(USER_RETRY_ROUTING_3);
  }

  @Bean
  public Binding bindingStaffRetry3() {
    return BindingBuilder.bind(staffRetryQueue3()).to(staffRetryExchange()).with(STAFF_RETRY_ROUTING_3);
  }

  @Bean
  public TopicExchange userDlx() {
    return new TopicExchange(USER_DLX);
  }

  @Bean
  public TopicExchange staffDlx() {
    return new TopicExchange(STAFF_DLX);
  }

  @Bean
  public Queue userDlq() {
    return new Queue(USER_DLQ, true);
  }

  @Bean
  public Queue staffDlq() {
    return new Queue(STAFF_DLQ, true);
  }

  @Bean
  public Binding bindingDlq() {
    return BindingBuilder.bind(userDlq()).to(userDlx()).with(USER_DLQ_ROUTING);
  }

  @Bean
  public Binding bindingStaffDlq() {
    return BindingBuilder.bind(staffDlq()).to(staffDlx()).with(STAFF_DLQ_ROUTING);
  }

  @Bean
  public Jackson2JsonMessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

}
