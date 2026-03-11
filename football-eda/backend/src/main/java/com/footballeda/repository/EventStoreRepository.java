package com.footballeda.repository;

import com.footballeda.domain.model.EventStoreEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventStoreRepository extends JpaRepository<EventStoreEntry, Long> {

    List<EventStoreEntry> findByMatchIdOrderByOccurredAtAsc(UUID matchId);

    boolean existsByEventId(UUID eventId);

    List<EventStoreEntry> findByEventType(String eventType);
}
