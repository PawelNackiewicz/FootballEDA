package com.footballeda.consumer;

import com.footballeda.config.RabbitMQConfig;
import com.footballeda.domain.event.*;
import com.footballeda.domain.model.ProcessedEvent;
import com.footballeda.repository.ProcessedEventRepository;
import com.footballeda.websocket.MatchWebSocketBroadcaster;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private static final String CONSUMER_NAME = "notifications";
    private static final int MAX_NOTIFICATIONS = 100;

    private final ProcessedEventRepository processedEventRepository;
    private final MatchWebSocketBroadcaster broadcaster;
    private final Deque<Map<String, Object>> notifications = new ConcurrentLinkedDeque<>();

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE)
    public void consume(MatchEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            if (processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), CONSUMER_NAME)) {
                log.debug("Duplicate event detected, skipping: {}", event.eventId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            String message = formatNotification(event);
            if (message != null) {
                Map<String, Object> notification = Map.of(
                        "id", event.eventId().toString(),
                        "message", message,
                        "type", event.type().name(),
                        "matchMinute", event.matchMinute(),
                        "timestamp", Instant.now().toString()
                );

                notifications.addFirst(notification);
                while (notifications.size() > MAX_NOTIFICATIONS) {
                    notifications.removeLast();
                }

                broadcaster.broadcastNotification(notification);
            }

            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(event.eventId())
                    .consumerName(CONSUMER_NAME)
                    .processedAt(Instant.now())
                    .build());

            channel.basicAck(deliveryTag, false);
            log.info("[{}] Processed event: type={}, eventId={}", CONSUMER_NAME, event.type(), event.eventId());

        } catch (Exception e) {
            log.error("[{}] Failed to process event: {}", CONSUMER_NAME, event.eventId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private String formatNotification(MatchEvent event) {
        return switch (event) {
            case GoalScored g -> String.format("GOAL! %s %d'%s",
                    g.scoringPlayerName(), g.matchMinute(), g.isOwnGoal() ? " (OG)" : "");
            case CardIssued c -> String.format("%s - %s %d'",
                    c.cardType().name().equals("YELLOW") ? "Yellow Card" : "Red Card",
                    c.playerName(), c.matchMinute());
            case SubstitutionMade s -> String.format("Substitution: %s -> %s %d'",
                    s.playerOutName(), s.playerInName(), s.matchMinute());
            case MatchStatusChanged m -> String.format("Match Status: %s", m.newStatus());
            case FoulCommitted f -> null; // No notification for fouls
        };
    }

    public Deque<Map<String, Object>> getNotifications() {
        return notifications;
    }
}
