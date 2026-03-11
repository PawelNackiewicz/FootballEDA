package com.footballeda.domain.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.footballeda.domain.enums.EventType;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GoalScored.class, name = "GOAL"),
        @JsonSubTypes.Type(value = CardIssued.class, name = "CARD"),
        @JsonSubTypes.Type(value = SubstitutionMade.class, name = "SUBSTITUTION"),
        @JsonSubTypes.Type(value = MatchStatusChanged.class, name = "STATUS_CHANGE"),
        @JsonSubTypes.Type(value = FoulCommitted.class, name = "FOUL")
})
public sealed interface MatchEvent permits GoalScored, CardIssued, SubstitutionMade, MatchStatusChanged, FoulCommitted {
    UUID eventId();
    UUID matchId();
    Instant occurredAt();
    int matchMinute();
    EventType type();
}
