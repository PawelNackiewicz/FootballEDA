package com.footballeda.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "standings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Standing {

    @Id
    private UUID id;

    @Column(name = "team_id", nullable = false, unique = true)
    private UUID teamId;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    @Builder.Default
    private int played = 0;

    @Builder.Default
    private int won = 0;

    @Builder.Default
    private int drawn = 0;

    @Builder.Default
    private int lost = 0;

    @Column(name = "goals_for")
    @Builder.Default
    private int goalsFor = 0;

    @Column(name = "goals_against")
    @Builder.Default
    private int goalsAgainst = 0;

    @Builder.Default
    private int points = 0;

    public int getGoalDifference() {
        return goalsFor - goalsAgainst;
    }
}
