package com.footballeda.domain.event;

import com.footballeda.domain.enums.EventType;

import java.time.Instant;
import java.util.UUID;

public record SubstitutionMade(
        UUID eventId,
        UUID matchId,
        Instant occurredAt,
        int matchMinute,
        UUID playerOutId,
        String playerOutName,
        UUID playerInId,
        String playerInName,
        UUID teamId
) implements MatchEvent {
    @Override
    public EventType type() {
        return EventType.SUBSTITUTION;
    }
}
