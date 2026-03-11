package com.footballeda.unit.consumer;

import com.footballeda.consumer.StandingsConsumer;
import com.footballeda.domain.enums.MatchStatus;
import com.footballeda.domain.event.GoalScored;
import com.footballeda.domain.event.MatchStatusChanged;
import com.footballeda.domain.model.Match;
import com.footballeda.domain.model.Team;
import com.footballeda.repository.MatchRepository;
import com.footballeda.repository.ProcessedEventRepository;
import com.footballeda.service.StandingsService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StandingsConsumerTest {

    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private StandingsService standingsService;
    @Mock private MatchRepository matchRepository;
    @Mock private Channel channel;

    @InjectMocks private StandingsConsumer consumer;

    private final UUID matchId = UUID.randomUUID();
    private final UUID homeTeamId = UUID.randomUUID();
    private final UUID awayTeamId = UUID.randomUUID();

    private GoalScored buildGoalEvent() {
        return new GoalScored(
                UUID.randomUUID(), matchId, Instant.now(), 67,
                UUID.randomUUID(), "Lewandowski", UUID.randomUUID(), "Pedri",
                homeTeamId, 1, 0, false
        );
    }

    private Match buildMatch() {
        return Match.builder()
                .id(matchId)
                .homeTeam(Team.builder().id(homeTeamId).name("FC Barcelona").shortName("BAR").build())
                .awayTeam(Team.builder().id(awayTeamId).name("Real Madrid").shortName("RMA").build())
                .build();
    }

    @Test
    void shouldIncrementGoalCountOnGoalScored() throws Exception {
        GoalScored event = buildGoalEvent();
        when(processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), "standings")).thenReturn(false);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(buildMatch()));

        consumer.consume(event, channel, 1L);

        verify(standingsService).updateOnGoal(eq(event), eq(homeTeamId), eq(awayTeamId));
        verify(channel).basicAck(1L, false);
    }

    @Test
    void shouldBeIdempotentOnDuplicateEvent() throws Exception {
        GoalScored event = buildGoalEvent();
        when(processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), "standings")).thenReturn(true);

        consumer.consume(event, channel, 1L);

        verify(standingsService, never()).updateOnGoal(any(), any(), any());
        verify(channel).basicAck(1L, false);
    }

    @Test
    void shouldAcknowledgeMessageOnSuccess() throws Exception {
        GoalScored event = buildGoalEvent();
        when(processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), "standings")).thenReturn(false);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(buildMatch()));

        consumer.consume(event, channel, 42L);

        verify(channel).basicAck(42L, false);
    }

    @Test
    void shouldNackMessageOnFailure() throws Exception {
        GoalScored event = buildGoalEvent();
        when(processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), "standings")).thenReturn(false);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(buildMatch()));
        doThrow(new RuntimeException("DB error")).when(standingsService).updateOnGoal(any(), any(), any());

        consumer.consume(event, channel, 1L);

        verify(channel).basicNack(1L, false, false);
    }
}
