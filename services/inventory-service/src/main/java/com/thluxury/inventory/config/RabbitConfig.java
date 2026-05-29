package com.thluxury.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thluxury.inventory.messaging.InventoryTopology;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(InventoryTopology.ORDER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue inventoryOrderPaidDlq() {
        return QueueBuilder.durable(InventoryTopology.ORDER_PAID_DLQ).build();
    }

    @Bean
    public Queue inventoryOrderPaidQueue() {
        return QueueBuilder.durable(InventoryTopology.ORDER_PAID_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", InventoryTopology.ORDER_PAID_DLQ)
                .build();
    }

    @Bean
    public Binding inventoryOrderPaidBinding(Queue inventoryOrderPaidQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(inventoryOrderPaidQueue)
                .to(orderExchange)
                .with(InventoryTopology.RK_ORDER_PAID);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        return t;
    }
}
