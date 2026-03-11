package com.footballeda.service;

import com.footballeda.domain.event.GoalScored;
import com.footballeda.domain.model.Standing;
import com.footballeda.repository.StandingsRepository;
import com.footballeda.websocket.MatchWebSocketBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StandingsService {

    private final StandingsRepository standingsRepository;
    private final MatchWebSocketBroadcaster broadcaster;

    @Transactional
    public void updateOnGoal(GoalScored event, UUID homeTeamId, UUID awayTeamId) {
        UUID scoringTeamId = event.teamId();
        UUID concedingTeamId = scoringTeamId.equals(homeTeamId) ? awayTeamId : homeTeamId;

        Standing scoringStanding = standingsRepository.findByTeamId(scoringTeamId)
                .orElseThrow(() -> new RuntimeException("Standing not found for team: " + scoringTeamId));
        scoringStanding.setGoalsFor(scoringStanding.getGoalsFor() + 1);
        standingsRepository.save(scoringStanding);

        Standing concedingStanding = standingsRepository.findByTeamId(concedingTeamId)
                .orElseThrow(() -> new RuntimeException("Standing not found for team: " + concedingTeamId));
        concedingStanding.setGoalsAgainst(concedingStanding.getGoalsAgainst() + 1);
        standingsRepository.save(concedingStanding);

        log.info("Updated standings on goal: {} scored, {} conceded", scoringTeamId, concedingTeamId);
        broadcastStandings();
    }

    @Transactional
    public void updateOnMatchEnd(UUID homeTeamId, UUID awayTeamId, int homeScore, int awayScore) {
        Standing homeStanding = standingsRepository.findByTeamId(homeTeamId)
                .orElseThrow(() -> new RuntimeException("Standing not found for team: " + homeTeamId));
        Standing awayStanding = standingsRepository.findByTeamId(awayTeamId)
                .orElseThrow(() -> new RuntimeException("Standing not found for team: " + awayTeamId));

        homeStanding.setPlayed(homeStanding.getPlayed() + 1);
        awayStanding.setPlayed(awayStanding.getPlayed() + 1);

        if (homeScore > awayScore) {
            homeStanding.setWon(homeStanding.getWon() + 1);
            homeStanding.setPoints(homeStanding.getPoints() + 3);
            awayStanding.setLost(awayStanding.getLost() + 1);
        } else if (homeScore < awayScore) {
            awayStanding.setWon(awayStanding.getWon() + 1);
            awayStanding.setPoints(awayStanding.getPoints() + 3);
            homeStanding.setLost(homeStanding.getLost() + 1);
        } else {
            homeStanding.setDrawn(homeStanding.getDrawn() + 1);
            homeStanding.setPoints(homeStanding.getPoints() + 1);
            awayStanding.setDrawn(awayStanding.getDrawn() + 1);
            awayStanding.setPoints(awayStanding.getPoints() + 1);
        }

        standingsRepository.save(homeStanding);
        standingsRepository.save(awayStanding);
        log.info("Updated standings on match end: {} vs {} ({}:{})", homeTeamId, awayTeamId, homeScore, awayScore);
        broadcastStandings();
    }

    public List<Standing> getStandings() {
        return standingsRepository.findAllByOrderByPointsDescGoalDifferenceDesc();
    }

    public void broadcastStandings() {
        broadcaster.broadcastStandings(getStandings());
    }

    @Transactional
    public void resetForTeams(UUID homeTeamId, UUID awayTeamId) {
        standingsRepository.findByTeamId(homeTeamId).ifPresent(s -> {
            s.setPlayed(0); s.setWon(0); s.setDrawn(0); s.setLost(0);
            s.setGoalsFor(0); s.setGoalsAgainst(0); s.setPoints(0);
            standingsRepository.save(s);
        });
        standingsRepository.findByTeamId(awayTeamId).ifPresent(s -> {
            s.setPlayed(0); s.setWon(0); s.setDrawn(0); s.setLost(0);
            s.setGoalsFor(0); s.setGoalsAgainst(0); s.setPoints(0);
            standingsRepository.save(s);
        });
    }
}
