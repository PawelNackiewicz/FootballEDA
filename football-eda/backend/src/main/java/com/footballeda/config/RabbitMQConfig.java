package com.footballeda.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "football.events";
    public static final String DLX_EXCHANGE_NAME = "football.dlx";

    public static final String STANDINGS_QUEUE = "standings.queue";
    public static final String STATS_QUEUE = "stats.queue";
    public static final String NOTIFICATIONS_QUEUE = "notifications.queue";
    public static final String AUDIT_QUEUE = "audit.queue";
    public static final String DLQ = "football.dlq";

    public static final String ROUTING_KEY_GOAL = "match.goal";
    public static final String ROUTING_KEY_CARD = "match.card";
    public static final String ROUTING_KEY_SUBSTITUTION = "match.substitution";
    public static final String ROUTING_KEY_STATUS = "match.status";
    public static final String ROUTING_KEY_FOUL = "match.foul";
    public static final String ROUTING_KEY_ALL = "match.#";

    @Bean
    public TopicExchange footballExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue standingsQueue() {
        return QueueBuilder.durable(STANDINGS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue statsQueue() {
        return QueueBuilder.durable(STATS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    // Standings queue bindings
    @Bean
    public Binding standingsGoalBinding() {
        return BindingBuilder.bind(standingsQueue()).to(footballExchange()).with(ROUTING_KEY_GOAL);
    }

    @Bean
    public Binding standingsStatusBinding() {
        return BindingBuilder.bind(standingsQueue()).to(footballExchange()).with(ROUTING_KEY_STATUS);
    }

    // Stats queue bindings (all events)
    @Bean
    public Binding statsAllBinding() {
        return BindingBuilder.bind(statsQueue()).to(footballExchange()).with(ROUTING_KEY_ALL);
    }

    // Notifications queue bindings
    @Bean
    public Binding notificationsGoalBinding() {
        return BindingBuilder.bind(notificationsQueue()).to(footballExchange()).with(ROUTING_KEY_GOAL);
    }

    @Bean
    public Binding notificationsCardBinding() {
        return BindingBuilder.bind(notificationsQueue()).to(footballExchange()).with(ROUTING_KEY_CARD);
    }

    @Bean
    public Binding notificationsStatusBinding() {
        return BindingBuilder.bind(notificationsQueue()).to(footballExchange()).with(ROUTING_KEY_STATUS);
    }

    @Bean
    public Binding notificationsSubstitutionBinding() {
        return BindingBuilder.bind(notificationsQueue()).to(footballExchange()).with(ROUTING_KEY_SUBSTITUTION);
    }

    // Audit queue bindings (all events)
    @Bean
    public Binding auditAllBinding() {
        return BindingBuilder.bind(auditQueue()).to(footballExchange()).with(ROUTING_KEY_ALL);
    }

    // DLQ binding
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);
        return factory;
    }
}
