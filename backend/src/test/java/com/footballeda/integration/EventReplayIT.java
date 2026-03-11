package com.footballeda.integration;

import com.footballeda.domain.enums.MatchStatus;
import com.footballeda.domain.event.GoalScored;
import com.footballeda.domain.event.MatchStatusChanged;
import com.footballeda.domain.model.Match;
import com.footballeda.domain.model.Team;
import com.footballeda.producer.MatchEventPublisher;
import com.footballeda.repository.*;
import com.footballeda.service.EventStoreService;
import com.footballeda.service.MatchReplayService;
import com.footballeda.service.MatchStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class EventReplayIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("football_eda_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @Autowired private MatchEventPublisher publisher;
    @Autowired private EventStoreService eventStoreService;
    @Autowired private MatchReplayService replayService;
    @Autowired private MatchRepository matchRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private MatchStatsService matchStatsService;
    @Autowired private EventStoreRepository eventStoreRepository;

    @Test
    void shouldRebuildIdenticalStateFromEventReplay() {
        // Setup: create teams and match
        UUID homeTeamId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID awayTeamId = UUID.fromString("10000000-0000-0000-0000-000000000002");

        Team homeTeam = teamRepository.findById(homeTeamId).orElseThrow();
        Team awayTeam = teamRepository.findById(awayTeamId).orElseThrow();

        Match match = Match.builder()
                .id(UUID.randomUUID())
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .status(MatchStatus.NOT_STARTED)
                .build();
        matchRepository.save(match);

        matchStatsService.initializeStats(match.getId(), homeTeamId, awayTeamId);

        // Step 1: Publish events directly to event store (simulating a completed match)
        GoalScored goal1 = new GoalScored(UUID.randomUUID(), match.getId(), Instant.now(), 25,
                UUID.randomUUID(), "Lewandowski", null, null, homeTeamId, 1, 0, false);
        GoalScored goal2 = new GoalScored(UUID.randomUUID(), match.getId(), Instant.now().plusSeconds(1), 70,
                UUID.randomUUID(), "Vinicius", null, null, awayTeamId, 1, 1, false);

        eventStoreService.save(goal1);
        eventStoreService.save(goal2);

        // Step 2: Replay
        Map<String, Object> result = replayService.replay(match.getId());

        // Step 3: Verify
        assertThat(result.get("eventsReplayed")).isEqualTo(2);
        assertThat(result.get("homeScore")).isEqualTo(1);
        assertThat(result.get("awayScore")).isEqualTo(1);
    }
}
