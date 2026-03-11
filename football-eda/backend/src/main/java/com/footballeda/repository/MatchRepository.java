package com.footballeda.repository;

import com.footballeda.domain.enums.MatchStatus;
import com.footballeda.domain.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findByStatus(MatchStatus status);
}
