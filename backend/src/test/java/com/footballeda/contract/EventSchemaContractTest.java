package com.footballeda.contract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.footballeda.domain.enums.CardType;
import com.footballeda.domain.enums.MatchStatus;
import com.footballeda.domain.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventSchemaContractTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldSerializeGoalScoredToExpectedSchema() throws JsonProcessingException {
        GoalScored event = new GoalScored(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now(), 67,
                UUID.randomUUID(), "Lewandowski", UUID.randomUUID(), "Pedri",
                UUID.randomUUID(), 1, 0, false
        );

        String json = objectMapper.writeValueAsString(event);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.has("eventId")).isTrue();
        assertThat(node.has("matchId")).isTrue();
        assertThat(node.has("occurredAt")).isTrue();
        assertThat(node.has("matchMinute")).isTrue();
        assertThat(node.get("type").asText()).isEqualTo("GOAL");
        assertThat(node.has("scoringPlayerName")).isTrue();
        assertThat(node.has("homeScore")).isTrue();
        assertThat(node.has("awayScore")).isTrue();
    }

    @Test
    void shouldSerializeCardIssuedToExpectedSchema() throws JsonProcessingException {
        CardIssued event = new CardIssued(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now(), 34,
                UUID.randomUUID(), "Muller", UUID.randomUUID(), CardType.YELLOW, "Tactical foul"
        );

        String json = objectMapper.writeValueAsString(event);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("type").asText()).isEqualTo("CARD");
        assertThat(node.has("cardType")).isTrue();
        assertThat(node.has("playerName")).isTrue();
    }

    @Test
    void shouldDeserializeMatchEventFromJson() throws JsonProcessingException {
        GoalScored original = new GoalScored(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now(), 55,
                UUID.randomUUID(), "Haaland", null, null,
                UUID.randomUUID(), 3, 1, false
        );

        String json = objectMapper.writeValueAsString(original);
        MatchEvent deserialized = objectMapper.readValue(json, MatchEvent.class);

        assertThat(deserialized).isInstanceOf(GoalScored.class);
        GoalScored goal = (GoalScored) deserialized;
        assertThat(goal.scoringPlayerName()).isEqualTo("Haaland");
        assertThat(goal.eventId()).isEqualTo(original.eventId());
    }

    @Test
    void shouldIncludeRequiredFieldsInAllEvents() throws JsonProcessingException {
        MatchEvent[] events = {
                new GoalScored(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), 10,
                        UUID.randomUUID(), "Test", null, null, UUID.randomUUID(), 1, 0, false),
                new CardIssued(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), 20,
                        UUID.randomUUID(), "Test", UUID.randomUUID(), CardType.RED, "Foul"),
                new SubstitutionMade(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), 60,
                        UUID.randomUUID(), "PlayerOut", UUID.randomUUID(), "PlayerIn", UUID.randomUUID()),
                new MatchStatusChanged(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), 45,
                        MatchStatus.FIRST_HALF, MatchStatus.HALF_TIME),
                new FoulCommitted(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), 35,
                        UUID.randomUUID(), "Test", UUID.randomUUID(), "MODERATE")
        };

        for (MatchEvent event : events) {
            String json = objectMapper.writeValueAsString(event);
            JsonNode node = objectMapper.readTree(json);

            assertThat(node.has("eventId")).as("eventId missing for %s", event.type()).isTrue();
            assertThat(node.has("matchId")).as("matchId missing for %s", event.type()).isTrue();
            assertThat(node.has("occurredAt")).as("occurredAt missing for %s", event.type()).isTrue();
            assertThat(node.has("matchMinute")).as("matchMinute missing for %s", event.type()).isTrue();
            assertThat(node.has("type")).as("type missing for %s", event.type()).isTrue();
        }
    }
}
