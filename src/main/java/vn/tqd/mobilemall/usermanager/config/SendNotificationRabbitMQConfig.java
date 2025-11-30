package vn.tqd.mobilemall.usermanager.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SendNotificationRabbitMQConfig {
    @Value("${queue.notification.routing-key}")
    private String routingKey;

    @Value("${queue.notification.queue}")
    private String queueName;

    @Value("${queue.notification.exchange}")
    private String exchangeName;

    @Bean
    DirectExchange sendEmailExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Queue sendEmailQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    Binding emailQueueBinding(Queue sendEmailQueue,
                              DirectExchange sendEmailExchange) {
        return BindingBuilder
                .bind(sendEmailQueue)
                .to(sendEmailExchange)
                .with(routingKey);
    }
}

