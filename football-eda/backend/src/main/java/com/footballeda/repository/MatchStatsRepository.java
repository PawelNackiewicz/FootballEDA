package com.footballeda.repository;

import com.footballeda.domain.model.MatchStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchStatsRepository extends JpaRepository<MatchStats, UUID> {

    Optional<MatchStats> findByMatchIdAndTeamId(UUID matchId, UUID teamId);

    List<MatchStats> findByMatchId(UUID matchId);
}
