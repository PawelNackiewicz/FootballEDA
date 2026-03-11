package com.footballeda.repository;

import com.footballeda.domain.model.Standing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StandingsRepository extends JpaRepository<Standing, UUID> {

    Optional<Standing> findByTeamId(UUID teamId);

    @Query("SELECT s FROM Standing s ORDER BY s.points DESC, (s.goalsFor - s.goalsAgainst) DESC, s.goalsFor DESC")
    List<Standing> findAllByOrderByPointsDescGoalDifferenceDesc();
}
