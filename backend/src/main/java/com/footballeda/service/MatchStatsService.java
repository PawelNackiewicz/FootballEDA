package com.footballeda.service;

import com.footballeda.domain.enums.CardType;
import com.footballeda.domain.event.*;
import com.footballeda.domain.model.MatchStats;
import com.footballeda.repository.MatchStatsRepository;
import com.footballeda.websocket.MatchWebSocketBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchStatsService {

    private final MatchStatsRepository matchStatsRepository;
    private final MatchWebSocketBroadcaster broadcaster;

    @Transactional
    public void initializeStats(UUID matchId, UUID homeTeamId, UUID awayTeamId) {
        if (matchStatsRepository.findByMatchIdAndTeamId(matchId, homeTeamId).isEmpty()) {
            matchStatsRepository.save(MatchStats.builder()
                    .id(UUID.randomUUID())
                    .matchId(matchId)
                    .teamId(homeTeamId)
                    .build());
        }
        if (matchStatsRepository.findByMatchIdAndTeamId(matchId, awayTeamId).isEmpty()) {
            matchStatsRepository.save(MatchStats.builder()
                    .id(UUID.randomUUID())
                    .matchId(matchId)
                    .teamId(awayTeamId)
                    .build());
        }
    }

    @Transactional
    public void updateStats(MatchEvent event) {
        switch (event) {
            case GoalScored goal -> {
                MatchStats stats = getOrCreateStats(goal.matchId(), goal.teamId());
                stats.setShots(stats.getShots() + 1);
                stats.setShotsOnTarget(stats.getShotsOnTarget() + 1);
                double xg = ThreadLocalRandom.current().nextDouble(0.3, 0.85);
                stats.setExpectedGoals(stats.getExpectedGoals() + xg);
                matchStatsRepository.save(stats);
                recalculatePossession(goal.matchId());
            }
            case FoulCommitted foul -> {
                MatchStats stats = getOrCreateStats(foul.matchId(), foul.teamId());
                stats.setFouls(stats.getFouls() + 1);
                matchStatsRepository.save(stats);
            }
            case CardIssued card -> {
                MatchStats stats = getOrCreateStats(card.matchId(), card.teamId());
                if (card.cardType() == CardType.YELLOW) {
                    stats.setYellowCards(stats.getYellowCards() + 1);
                } else {
                    stats.setRedCards(stats.getRedCards() + 1);
                }
                matchStatsRepository.save(stats);
            }
            default -> {
                // Other events don't affect stats
            }
        }
        broadcastStats(event.matchId());
    }

    public List<MatchStats> getStats(UUID matchId) {
        return matchStatsRepository.findByMatchId(matchId);
    }

    public void broadcastStats(UUID matchId) {
        broadcaster.broadcastStats(getStats(matchId));
    }

    @Transactional
    public void resetStats(UUID matchId) {
        List<MatchStats> statsList = matchStatsRepository.findByMatchId(matchId);
        for (MatchStats stats : statsList) {
            stats.setPossession(50.0);
            stats.setShots(0);
            stats.setShotsOnTarget(0);
            stats.setFouls(0);
            stats.setCorners(0);
            stats.setYellowCards(0);
            stats.setRedCards(0);
            stats.setExpectedGoals(0.0);
            matchStatsRepository.save(stats);
        }
    }

    private MatchStats getOrCreateStats(UUID matchId, UUID teamId) {
        return matchStatsRepository.findByMatchIdAndTeamId(matchId, teamId)
                .orElseGet(() -> matchStatsRepository.save(MatchStats.builder()
                        .id(UUID.randomUUID())
                        .matchId(matchId)
                        .teamId(teamId)
                        .build()));
    }

    private void recalculatePossession(UUID matchId) {
        List<MatchStats> statsList = matchStatsRepository.findByMatchId(matchId);
        if (statsList.size() == 2) {
            double home = 45 + ThreadLocalRandom.current().nextDouble(10);
            statsList.get(0).setPossession(Math.round(home * 10.0) / 10.0);
            statsList.get(1).setPossession(Math.round((100 - home) * 10.0) / 10.0);
            matchStatsRepository.saveAll(statsList);
        }
    }
}
