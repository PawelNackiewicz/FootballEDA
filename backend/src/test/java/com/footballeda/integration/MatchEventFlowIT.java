package com.footballeda.integration;

import com.footballeda.domain.event.GoalScored;
import com.footballeda.producer.MatchEventPublisher;
import com.footballeda.repository.EventStoreRepository;
import com.footballeda.repository.MatchStatsRepository;
import com.footballeda.repository.StandingsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

@SpringBootTest
@Testcontainers
class MatchEventFlowIT {

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
    @Autowired private EventStoreRepository eventStoreRepository;
    @Autowired private StandingsRepository standingsRepository;
    @Autowired private MatchStatsRepository matchStatsRepository;

    @Test
    void shouldFanOutGoalEventToAllConsumers() {
        UUID matchId = UUID.fromString("10000000-0000-0000-0000-000000000099");
        UUID teamId = UUID.fromString("10000000-0000-0000-0000-000000000001");

        GoalScored event = new GoalScored(
                UUID.randomUUID(), matchId, Instant.now(), 67,
                UUID.randomUUID(), "Lewandowski", UUID.randomUUID(), "Pedri",
                teamId, 1, 0, false
        );

        publisher.publish(event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(eventStoreRepository.existsByEventId(event.eventId())).isTrue();
        });
    }
}
