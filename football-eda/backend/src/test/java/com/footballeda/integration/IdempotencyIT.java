package com.footballeda.integration;

import com.footballeda.domain.event.GoalScored;
import com.footballeda.producer.MatchEventPublisher;
import com.footballeda.repository.EventStoreRepository;
import com.footballeda.repository.ProcessedEventRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class IdempotencyIT {

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
    @Autowired private ProcessedEventRepository processedEventRepository;

    @Test
    void shouldHandleDuplicateEventsGracefully() {
        UUID eventId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        GoalScored event = new GoalScored(
                eventId, matchId, Instant.now(), 30,
                UUID.randomUUID(), "Haaland", null, null,
                teamId, 1, 0, false
        );

        // Publish same event 3 times
        publisher.publish(event);
        publisher.publish(event);
        publisher.publish(event);

        // Wait for processing
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(processedEventRepository.existsByEventIdAndConsumerName(eventId, "audit")).isTrue();
        });

        // Event should only be stored once
        long count = eventStoreRepository.findByMatchIdOrderByOccurredAtAsc(matchId).size();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldProcessDifferentEventsWithDifferentIds() {
        UUID matchId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            GoalScored event = new GoalScored(
                    UUID.randomUUID(), matchId, Instant.now(), 30 + i * 10,
                    UUID.randomUUID(), "Player" + i, null, null,
                    teamId, i + 1, 0, false
            );
            publisher.publish(event);
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            long count = eventStoreRepository.findByMatchIdOrderByOccurredAtAsc(matchId).size();
            assertThat(count).isEqualTo(3);
        });
    }
}
