package com.footballeda.domain.model;

import com.footballeda.domain.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @Column(name = "home_score")
    @Builder.Default
    private int homeScore = 0;

    @Column(name = "away_score")
    @Builder.Default
    private int awayScore = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MatchStatus status = MatchStatus.NOT_STARTED;

    @Column(name = "current_minute")
    @Builder.Default
    private int currentMinute = 0;

    @Column(name = "started_at")
    private Instant startedAt;
}
