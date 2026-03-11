package com.footballeda.domain.event;

import com.footballeda.domain.enums.EventType;

import java.time.Instant;
import java.util.UUID;

public record FoulCommitted(
        UUID eventId,
        UUID matchId,
        Instant occurredAt,
        int matchMinute,
        UUID playerId,
        String playerName,
        UUID teamId,
        String severity
) implements MatchEvent {
    @Override
    public EventType type() {
        return EventType.FOUL;
    }
}
