package com.footballeda.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "match_stats", uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "team_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchStats {

    @Id
    private UUID id;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Builder.Default
    private double possession = 50.0;

    @Builder.Default
    private int shots = 0;

    @Column(name = "shots_on_target")
    @Builder.Default
    private int shotsOnTarget = 0;

    @Builder.Default
    private int fouls = 0;

    @Builder.Default
    private int corners = 0;

    @Column(name = "yellow_cards")
    @Builder.Default
    private int yellowCards = 0;

    @Column(name = "red_cards")
    @Builder.Default
    private int redCards = 0;

    @Column(name = "expected_goals")
    @Builder.Default
    private double expectedGoals = 0.0;
}
