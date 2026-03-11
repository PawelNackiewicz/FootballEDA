package com.footballeda.producer;

import com.footballeda.domain.enums.CardType;
import com.footballeda.domain.enums.MatchStatus;
import com.footballeda.domain.event.*;
import com.footballeda.domain.model.Match;
import com.footballeda.domain.model.Player;
import com.footballeda.repository.MatchRepository;
import com.footballeda.repository.PlayerRepository;
import com.footballeda.repository.TeamRepository;
import com.footballeda.service.EventStoreService;
import com.footballeda.service.MatchStatsService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchSimulator {

    private final MatchEventPublisher publisher;
    private final EventStoreService eventStoreService;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final MatchStatsService matchStatsService;

    @Getter
    private volatile Match currentMatch;
    private final AtomicInteger currentMinute = new AtomicInteger(0);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicInteger speedMultiplier = new AtomicInteger(1);

    private int homeScore = 0;
    private int awayScore = 0;
    private List<Player> homePlayers = new ArrayList<>();
    private List<Player> awayPlayers = new ArrayList<>();
    private final Set<Integer> substitutionMinutes = Set.of(46, 60, 70, 75, 80, 85);

    public synchronized void startMatch(UUID matchId) {
        if (isRunning.get()) {
            throw new IllegalStateException("A match is already running");
        }

        currentMatch = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));

        homePlayers = playerRepository.findByTeamId(currentMatch.getHomeTeam().getId());
        awayPlayers = playerRepository.findByTeamId(currentMatch.getAwayTeam().getId());

        homeScore = 0;
        awayScore = 0;
        currentMinute.set(0);
        isRunning.set(true);
        isPaused.set(false);

        currentMatch.setStatus(MatchStatus.NOT_STARTED);
        currentMatch.setHomeScore(0);
        currentMatch.setAwayScore(0);
        currentMatch.setStartedAt(Instant.now());
        matchRepository.save(currentMatch);

        matchStatsService.initializeStats(matchId, currentMatch.getHomeTeam().getId(), currentMatch.getAwayTeam().getId());

        log.info("Match started: {} vs {}", currentMatch.getHomeTeam().getName(), currentMatch.getAwayTeam().getName());

        // Emit kick-off status change
        publishStatusChange(MatchStatus.NOT_STARTED, MatchStatus.FIRST_HALF, 0);
    }

    public synchronized void startNewMatch() {
        var teams = teamRepository.findAll();
        if (teams.size() < 2) {
            throw new RuntimeException("Not enough teams in database");
        }
        Collections.shuffle(teams);

        Match match = Match.builder()
                .id(UUID.randomUUID())
                .homeTeam(teams.get(0))
                .awayTeam(teams.get(1))
                .status(MatchStatus.NOT_STARTED)
                .build();
        matchRepository.save(match);
        startMatch(match.getId());
    }

    public void pauseMatch() {
        isPaused.set(true);
        log.info("Match paused at minute {}", currentMinute.get());
    }

    public void resumeMatch() {
        isPaused.set(false);
        log.info("Match resumed at minute {}", currentMinute.get());
    }

    public synchronized void stopMatch() {
        isRunning.set(false);
        isPaused.set(false);
        if (currentMatch != null) {
            currentMatch.setStatus(MatchStatus.FULL_TIME);
            matchRepository.save(currentMatch);
        }
        log.info("Match stopped");
    }

    public void setSpeed(int multiplier) {
        if (multiplier < 1 || multiplier > 20) {
            throw new IllegalArgumentException("Speed must be between 1 and 20");
        }
        speedMultiplier.set(multiplier);
        log.info("Speed set to {}x", multiplier);
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isRunning", isRunning.get());
        status.put("isPaused", isPaused.get());
        status.put("currentMinute", currentMinute.get());
        status.put("speed", speedMultiplier.get());
        status.put("matchId", currentMatch != null ? currentMatch.getId() : null);
        status.put("homeTeam", currentMatch != null ? currentMatch.getHomeTeam().getName() : null);
        status.put("awayTeam", currentMatch != null ? currentMatch.getAwayTeam().getName() : null);
        status.put("homeScore", homeScore);
        status.put("awayScore", awayScore);
        return status;
    }

    @Scheduled(fixedDelay = 1000)
    public void tick() {
        if (!isRunning.get() || isPaused.get() || currentMatch == null) {
            return;
        }

        int speed = speedMultiplier.get();
        for (int i = 0; i < speed; i++) {
            int minute = currentMinute.incrementAndGet();
            if (minute > 90) {
                publishStatusChange(MatchStatus.SECOND_HALF, MatchStatus.FULL_TIME, 90);
                stopMatch();
                return;
            }

            // Status transitions
            if (minute == 45) {
                publishStatusChange(MatchStatus.FIRST_HALF, MatchStatus.HALF_TIME, 45);
            } else if (minute == 46) {
                publishStatusChange(MatchStatus.HALF_TIME, MatchStatus.SECOND_HALF, 46);
            }

            // Update match minute
            currentMatch.setCurrentMinute(minute);
            matchRepository.save(currentMatch);

            generateRandomEvents(minute);
        }
    }

    private void generateRandomEvents(int minute) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Goal probability ~3% per minute
        if (random.nextDouble() < 0.03) {
            generateGoal(minute);
        }

        // Foul probability ~10% per minute
        if (random.nextDouble() < 0.10) {
            generateFoul(minute);
        }

        // Card probability ~2% per minute (independent of fouls for simulation)
        if (random.nextDouble() < 0.02) {
            generateCard(minute);
        }

        // Substitutions at specific minutes
        if (substitutionMinutes.contains(minute)) {
            if (random.nextBoolean()) generateSubstitution(minute, true);
            if (random.nextBoolean()) generateSubstitution(minute, false);
        }
    }

    private void generateGoal(int minute) {
        boolean isHome = ThreadLocalRandom.current().nextBoolean();
        List<Player> players = isHome ? homePlayers : awayPlayers;
        if (players.isEmpty()) return;

        Player scorer = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        Player assister = players.stream()
                .filter(p -> !p.getId().equals(scorer.getId()))
                .findAny().orElse(null);

        if (isHome) homeScore++; else awayScore++;

        UUID teamId = isHome ? currentMatch.getHomeTeam().getId() : currentMatch.getAwayTeam().getId();

        GoalScored event = new GoalScored(
                UUID.randomUUID(), currentMatch.getId(), Instant.now(), minute,
                scorer.getId(), scorer.getName(),
                assister != null ? assister.getId() : null,
                assister != null ? assister.getName() : null,
                teamId, homeScore, awayScore, false
        );

        currentMatch.setHomeScore(homeScore);
        currentMatch.setAwayScore(awayScore);
        matchRepository.save(currentMatch);

        eventStoreService.save(event);
        publisher.publish(event);
        log.info("GOAL! {} scores at minute {} ({}-{})", scorer.getName(), minute, homeScore, awayScore);
    }

    private void generateFoul(int minute) {
        boolean isHome = ThreadLocalRandom.current().nextBoolean();
        List<Player> players = isHome ? homePlayers : awayPlayers;
        if (players.isEmpty()) return;

        Player fouler = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        UUID teamId = isHome ? currentMatch.getHomeTeam().getId() : currentMatch.getAwayTeam().getId();
        String[] severities = {"LIGHT", "MODERATE", "SERIOUS"};

        FoulCommitted event = new FoulCommitted(
                UUID.randomUUID(), currentMatch.getId(), Instant.now(), minute,
                fouler.getId(), fouler.getName(), teamId,
                severities[ThreadLocalRandom.current().nextInt(severities.length)]
        );

        eventStoreService.save(event);
        publisher.publish(event);
    }

    private void generateCard(int minute) {
        boolean isHome = ThreadLocalRandom.current().nextBoolean();
        List<Player> players = isHome ? homePlayers : awayPlayers;
        if (players.isEmpty()) return;

        Player player = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        UUID teamId = isHome ? currentMatch.getHomeTeam().getId() : currentMatch.getAwayTeam().getId();
        CardType cardType = ThreadLocalRandom.current().nextDouble() < 0.85 ? CardType.YELLOW : CardType.RED;

        CardIssued event = new CardIssued(
                UUID.randomUUID(), currentMatch.getId(), Instant.now(), minute,
                player.getId(), player.getName(), teamId, cardType, "Unsporting behavior"
        );

        eventStoreService.save(event);
        publisher.publish(event);
        log.info("{} card for {} at minute {}", cardType, player.getName(), minute);
    }

    private void generateSubstitution(int minute, boolean isHome) {
        List<Player> players = isHome ? homePlayers : awayPlayers;
        if (players.size() < 2) return;

        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        Player playerOut = shuffled.get(0);
        Player playerIn = shuffled.get(1);
        UUID teamId = isHome ? currentMatch.getHomeTeam().getId() : currentMatch.getAwayTeam().getId();

        SubstitutionMade event = new SubstitutionMade(
                UUID.randomUUID(), currentMatch.getId(), Instant.now(), minute,
                playerOut.getId(), playerOut.getName(),
                playerIn.getId(), playerIn.getName(), teamId
        );

        eventStoreService.save(event);
        publisher.publish(event);
    }

    private void publishStatusChange(MatchStatus oldStatus, MatchStatus newStatus, int minute) {
        MatchStatusChanged event = new MatchStatusChanged(
                UUID.randomUUID(), currentMatch.getId(), Instant.now(), minute, oldStatus, newStatus
        );

        currentMatch.setStatus(newStatus);
        matchRepository.save(currentMatch);

        eventStoreService.save(event);
        publisher.publish(event);
        log.info("Match status changed: {} -> {} at minute {}", oldStatus, newStatus, minute);
    }
}
