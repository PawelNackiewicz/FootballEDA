package com.footballeda.unit.consumer;

import com.footballeda.consumer.StatsConsumer;
import com.footballeda.domain.enums.CardType;
import com.footballeda.domain.event.CardIssued;
import com.footballeda.domain.event.FoulCommitted;
import com.footballeda.domain.event.GoalScored;
import com.footballeda.repository.ProcessedEventRepository;
import com.footballeda.service.MatchStatsService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsConsumerTest {

    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private MatchStatsService matchStatsService;
    @Mock private Channel channel;

    @InjectMocks private StatsConsumer consumer;

    private final UUID matchId = UUID.randomUUID();

    @Test
    void shouldUpdateShotsOnGoalScored() throws Exception {
        GoalScored event = new GoalScored(
                UUID.randomUUID(), matchId, Instant.now(), 55,
                UUID.randomUUID(), "Kane", null, null,
                UUID.randomUUID(), 2, 1, false
        );
        when(processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), "stats")).thenReturn(false);

        consumer.consume(event, channel, 1L);

        verify(matchStatsService).updateStats(event);
        verify(channel).basicAck(1L, false);
    }

    @Test
    void shouldUpdateFoulsOnFoulCommitted() throws Exception {
        FoulCommitted event = new FoulCommitted(
                UUID.randomUUID(), matchId, Instant.now(), 30,
                UUID.randomUUID(), "Casemiro", UUID.randomUUID(), "MODERATE"
        );
        when(processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), "stats")).thenReturn(false);

        consumer.consume(event, channel, 1L);

        verify(matchStatsService).updateStats(event);
    }

    @Test
    void shouldUpdateCardsOnCardIssued() throws Exception {
        CardIssued event = new CardIssued(
                UUID.randomUUID(), matchId, Instant.now(), 44,
                UUID.randomUUID(), "Ramos", UUID.randomUUID(), CardType.YELLOW, "Tactical foul"
        );
        when(processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), "stats")).thenReturn(false);

        consumer.consume(event, channel, 1L);

        verify(matchStatsService).updateStats(event);
    }

    @Test
    void shouldBeIdempotentOnDuplicateEvent() throws Exception {
        GoalScored event = new GoalScored(
                UUID.randomUUID(), matchId, Instant.now(), 55,
                UUID.randomUUID(), "Kane", null, null,
                UUID.randomUUID(), 2, 1, false
        );
        when(processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), "stats")).thenReturn(true);

        consumer.consume(event, channel, 1L);

        verify(matchStatsService, never()).updateStats(any());
    }
}
