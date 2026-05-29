package com.thluxury.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.events";
    public static final String AUTH_EXCHANGE = "auth.events";

    public static final String QUEUE_ORDER_CONFIRMED = "notify.order.confirmed.q";
    public static final String QUEUE_ORDER_STATUS = "notify.order.status.q";
    public static final String QUEUE_ORDER_FAILED = "notify.order.failed.q";
    public static final String QUEUE_AUTH_RESET = "notify.auth.reset.q";

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        // Producers gắn __TypeId__=LinkedHashMap; ép convert theo kiểu tham số listener (JsonNode)
        // thay vì theo type header → tránh MessageConversionException.
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(AUTH_EXCHANGE);
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_CONFIRMED)
                .withArgument("x-dead-letter-exchange", ORDER_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", QUEUE_ORDER_CONFIRMED + ".dlq")
                .build();
    }

    @Bean
    public Queue orderStatusQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_STATUS)
                .withArgument("x-dead-letter-exchange", ORDER_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", QUEUE_ORDER_STATUS + ".dlq")
                .build();
    }

    @Bean
    public Queue orderFailedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_FAILED)
                .withArgument("x-dead-letter-exchange", ORDER_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", QUEUE_ORDER_FAILED + ".dlq")
                .build();
    }

    @Bean
    public Queue authResetQueue() {
        return QueueBuilder.durable(QUEUE_AUTH_RESET)
                .withArgument("x-dead-letter-exchange", AUTH_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", QUEUE_AUTH_RESET + ".dlq")
                .build();
    }

    @Bean
    public Binding bindingOrderConfirmed(Queue orderConfirmedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderConfirmedQueue).to(orderExchange).with("order.paid");
    }

    @Bean
    public Binding bindingOrderStatus(Queue orderStatusQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderStatusQueue).to(orderExchange).with("order.status.changed");
    }

    @Bean
    public Binding bindingOrderFailed(Queue orderFailedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderFailedQueue).to(orderExchange).with("order.failed");
    }

    @Bean
    public Binding bindingAuthReset(Queue authResetQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(authResetQueue).to(authExchange).with("auth.password.reset");
    }

}
