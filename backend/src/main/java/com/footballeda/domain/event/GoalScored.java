package com.footballeda.domain.event;

import com.footballeda.domain.enums.EventType;

import java.time.Instant;
import java.util.UUID;

public record GoalScored(
        UUID eventId,
        UUID matchId,
        Instant occurredAt,
        int matchMinute,
        UUID scoringPlayerId,
        String scoringPlayerName,
        UUID assistPlayerId,
        String assistPlayerName,
        UUID teamId,
        int homeScore,
        int awayScore,
        boolean isOwnGoal
) implements MatchEvent {
    @Override
    public EventType type() {
        return EventType.GOAL;
    }
}
