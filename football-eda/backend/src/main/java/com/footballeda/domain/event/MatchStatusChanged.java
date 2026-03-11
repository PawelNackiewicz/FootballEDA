package com.footballeda.domain.event;

import com.footballeda.domain.enums.EventType;
import com.footballeda.domain.enums.MatchStatus;

import java.time.Instant;
import java.util.UUID;

public record MatchStatusChanged(
        UUID eventId,
        UUID matchId,
        Instant occurredAt,
        int matchMinute,
        MatchStatus oldStatus,
        MatchStatus newStatus
) implements MatchEvent {
    @Override
    public EventType type() {
        return EventType.STATUS_CHANGE;
    }
}
