package com.thluxury.catalog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thluxury.catalog.messaging.CatalogTopology;
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
    public TopicExchange catalogExchange() {
        return ExchangeBuilder.topicExchange(CatalogTopology.EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue catalogProjectionDlq() {
        return QueueBuilder.durable(CatalogTopology.PROJECTION_DLQ).build();
    }

    @Bean
    public Queue catalogProjectionQueue() {
        return QueueBuilder.durable(CatalogTopology.PROJECTION_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", CatalogTopology.PROJECTION_DLQ)
                .build();
    }

    @Bean
    public Binding catalogProjectionBinding(Queue catalogProjectionQueue, TopicExchange catalogExchange) {
        return BindingBuilder.bind(catalogProjectionQueue)
                .to(catalogExchange)
                .with(CatalogTopology.RK_PRODUCT_ANY);
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
        t.setMandatory(true);
        return t;
    }
}
