package com.footballeda.domain.event;

import com.footballeda.domain.enums.CardType;
import com.footballeda.domain.enums.EventType;

import java.time.Instant;
import java.util.UUID;

public record CardIssued(
        UUID eventId,
        UUID matchId,
        Instant occurredAt,
        int matchMinute,
        UUID playerId,
        String playerName,
        UUID teamId,
        CardType cardType,
        String reason
) implements MatchEvent {
    @Override
    public EventType type() {
        return EventType.CARD;
    }
}
