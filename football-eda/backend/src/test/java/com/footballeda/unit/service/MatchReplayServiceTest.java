package com.footballeda.unit.service;

import com.footballeda.domain.enums.MatchStatus;
import com.footballeda.domain.event.GoalScored;
import com.footballeda.domain.event.MatchEvent;
import com.footballeda.domain.model.EventStoreEntry;
import com.footballeda.domain.model.Match;
import com.footballeda.domain.model.Team;
import com.footballeda.repository.MatchRepository;
import com.footballeda.service.EventStoreService;
import com.footballeda.service.MatchReplayService;
import com.footballeda.service.MatchStatsService;
import com.footballeda.service.StandingsService;
import com.footballeda.websocket.MatchWebSocketBroadcaster;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchReplayServiceTest {

    @Mock private EventStoreService eventStoreService;
    @Mock private StandingsService standingsService;
    @Mock private MatchStatsService matchStatsService;
    @Mock private MatchRepository matchRepository;
    @Mock private MatchWebSocketBroadcaster broadcaster;

    @InjectMocks private MatchReplayService replayService;

    private final UUID matchId = UUID.randomUUID();
    private final UUID homeTeamId = UUID.randomUUID();
    private final UUID awayTeamId = UUID.randomUUID();

    private Match buildMatch() {
        return Match.builder()
                .id(matchId)
                .homeTeam(Team.builder().id(homeTeamId).name("FC Barcelona").shortName("BAR").build())
                .awayTeam(Team.builder().id(awayTeamId).name("Real Madrid").shortName("RMA").build())
                .homeScore(2).awayScore(1)
                .status(MatchStatus.FULL_TIME)
                .build();
    }

    @Test
    void shouldReplayAllEventsInOrder() {
        Match match = buildMatch();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        GoalScored goal1 = new GoalScored(UUID.randomUUID(), matchId, Instant.now(), 30,
                UUID.randomUUID(), "Lewandowski", null, null, homeTeamId, 1, 0, false);
        GoalScored goal2 = new GoalScored(UUID.randomUUID(), matchId, Instant.now().plusSeconds(60), 55,
                UUID.randomUUID(), "Vinicius", null, null, awayTeamId, 1, 1, false);

        EventStoreEntry entry1 = EventStoreEntry.builder().eventId(goal1.eventId()).matchId(matchId)
                .eventType("GOAL").payload("{}").occurredAt(goal1.occurredAt()).build();
        EventStoreEntry entry2 = EventStoreEntry.builder().eventId(goal2.eventId()).matchId(matchId)
                .eventType("GOAL").payload("{}").occurredAt(goal2.occurredAt()).build();

        when(eventStoreService.getEvents(matchId)).thenReturn(List.of(entry1, entry2));
        when(eventStoreService.deserializeEvent(entry1)).thenReturn(goal1);
        when(eventStoreService.deserializeEvent(entry2)).thenReturn(goal2);

        Map<String, Object> result = replayService.replay(matchId);

        assertThat(result).containsEntry("eventsReplayed", 2);
        verify(standingsService, times(2)).updateOnGoal(any(), eq(homeTeamId), eq(awayTeamId));
    }

    @Test
    void shouldResetStateBeforeReplay() {
        Match match = buildMatch();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(eventStoreService.getEvents(matchId)).thenReturn(Collections.emptyList());

        replayService.replay(matchId);

        verify(standingsService).resetForTeams(homeTeamId, awayTeamId);
        verify(matchStatsService).resetStats(matchId);
    }

    @Test
    void shouldHandleEmptyEventStore() {
        Match match = buildMatch();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(eventStoreService.getEvents(matchId)).thenReturn(Collections.emptyList());

        Map<String, Object> result = replayService.replay(matchId);

        assertThat(result).containsEntry("eventsReplayed", 0);
    }
}
