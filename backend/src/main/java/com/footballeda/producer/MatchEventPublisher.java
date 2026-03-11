package com.footballeda.producer;

import com.footballeda.config.RabbitMQConfig;
import com.footballeda.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(MatchEvent event) {
        String routingKey = resolveRoutingKey(event);
        log.info("Publishing event [{}] with routing key [{}], eventId={}", event.type(), routingKey, event.eventId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, event);
    }

    private String resolveRoutingKey(MatchEvent event) {
        return switch (event) {
            case GoalScored g -> RabbitMQConfig.ROUTING_KEY_GOAL;
            case CardIssued c -> RabbitMQConfig.ROUTING_KEY_CARD;
            case SubstitutionMade s -> RabbitMQConfig.ROUTING_KEY_SUBSTITUTION;
            case MatchStatusChanged m -> RabbitMQConfig.ROUTING_KEY_STATUS;
            case FoulCommitted f -> RabbitMQConfig.ROUTING_KEY_FOUL;
        };
    }
}
