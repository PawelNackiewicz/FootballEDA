package com.footballeda.consumer;

import com.footballeda.config.RabbitMQConfig;
import com.footballeda.domain.enums.MatchStatus;
import com.footballeda.domain.event.GoalScored;
import com.footballeda.domain.event.MatchEvent;
import com.footballeda.domain.event.MatchStatusChanged;
import com.footballeda.domain.model.Match;
import com.footballeda.domain.model.ProcessedEvent;
import com.footballeda.repository.MatchRepository;
import com.footballeda.repository.ProcessedEventRepository;
import com.footballeda.service.StandingsService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class StandingsConsumer {

    private static final String CONSUMER_NAME = "standings";

    private final ProcessedEventRepository processedEventRepository;
    private final StandingsService standingsService;
    private final MatchRepository matchRepository;

    @RabbitListener(queues = RabbitMQConfig.STANDINGS_QUEUE)
    public void consume(MatchEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            if (processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), CONSUMER_NAME)) {
                log.debug("Duplicate event detected, skipping: {}", event.eventId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            switch (event) {
                case GoalScored goal -> {
                    Match match = matchRepository.findById(goal.matchId()).orElse(null);
                    if (match != null) {
                        standingsService.updateOnGoal(goal, match.getHomeTeam().getId(), match.getAwayTeam().getId());
                    }
                }
                case MatchStatusChanged statusChanged -> {
                    if (statusChanged.newStatus() == MatchStatus.FULL_TIME) {
                        Match match = matchRepository.findById(statusChanged.matchId()).orElse(null);
                        if (match != null) {
                            standingsService.updateOnMatchEnd(
                                    match.getHomeTeam().getId(), match.getAwayTeam().getId(),
                                    match.getHomeScore(), match.getAwayScore());
                        }
                    }
                }
                default -> log.debug("Ignoring event type {} for standings", event.type());
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
}
