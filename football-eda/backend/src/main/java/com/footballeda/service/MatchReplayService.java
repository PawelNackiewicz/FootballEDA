package com.footballeda.service;

import com.footballeda.domain.event.GoalScored;
import com.footballeda.domain.event.MatchEvent;
import com.footballeda.domain.event.MatchStatusChanged;
import com.footballeda.domain.enums.MatchStatus;
import com.footballeda.domain.model.EventStoreEntry;
import com.footballeda.domain.model.Match;
import com.footballeda.repository.MatchRepository;
import com.footballeda.websocket.MatchWebSocketBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchReplayService {

    private final EventStoreService eventStoreService;
    private final StandingsService standingsService;
    private final MatchStatsService matchStatsService;
    private final MatchRepository matchRepository;
    private final MatchWebSocketBroadcaster broadcaster;

    @Transactional
    public Map<String, Object> replay(UUID matchId) {
        log.info("Starting replay for match: {}", matchId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));

        // Reset derived state
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setStatus(MatchStatus.NOT_STARTED);
        match.setCurrentMinute(0);
        matchRepository.save(match);

        standingsService.resetForTeams(match.getHomeTeam().getId(), match.getAwayTeam().getId());
        matchStatsService.resetStats(matchId);

        // Replay events in order
        List<EventStoreEntry> entries = eventStoreService.getEvents(matchId);
        log.info("Replaying {} events for match {}", entries.size(), matchId);

        for (EventStoreEntry entry : entries) {
            MatchEvent event = eventStoreService.deserializeEvent(entry);
            replayEvent(event, match);
        }

        matchRepository.save(match);
        standingsService.broadcastStandings();
        matchStatsService.broadcastStats(matchId);

        log.info("Replay complete for match {}. Final score: {} - {}", matchId, match.getHomeScore(), match.getAwayScore());

        return Map.of(
                "matchId", matchId,
                "homeScore", match.getHomeScore(),
                "awayScore", match.getAwayScore(),
                "status", match.getStatus(),
                "eventsReplayed", entries.size()
        );
    }

    private void replayEvent(MatchEvent event, Match match) {
        switch (event) {
            case GoalScored goal -> {
                match.setHomeScore(goal.homeScore());
                match.setAwayScore(goal.awayScore());
                match.setCurrentMinute(goal.matchMinute());
                standingsService.updateOnGoal(goal, match.getHomeTeam().getId(), match.getAwayTeam().getId());
            }
            case MatchStatusChanged statusChanged -> {
                match.setStatus(statusChanged.newStatus());
                match.setCurrentMinute(statusChanged.matchMinute());
                if (statusChanged.newStatus() == MatchStatus.FULL_TIME) {
                    standingsService.updateOnMatchEnd(
                            match.getHomeTeam().getId(), match.getAwayTeam().getId(),
                            match.getHomeScore(), match.getAwayScore());
                }
            }
            default -> {
                matchStatsService.updateStats(event);
                match.setCurrentMinute(event.matchMinute());
            }
        }
    }
}
