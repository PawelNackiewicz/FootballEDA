package com.footballeda.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballeda.domain.event.MatchEvent;
import com.footballeda.domain.model.EventStoreEntry;
import com.footballeda.repository.EventStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventStoreService {

    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(MatchEvent event) {
        if (eventStoreRepository.existsByEventId(event.eventId())) {
            log.debug("Event already exists in store, skipping: {}", event.eventId());
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(event);
            EventStoreEntry entry = EventStoreEntry.builder()
                    .eventId(event.eventId())
                    .matchId(event.matchId())
                    .eventType(event.type().name())
                    .payload(payload)
                    .occurredAt(event.occurredAt())
                    .processedAt(Instant.now())
                    .build();
            eventStoreRepository.save(entry);
            log.info("Saved event to store: type={}, eventId={}", event.type(), event.eventId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", event.eventId(), e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    public List<EventStoreEntry> getEvents(UUID matchId) {
        return eventStoreRepository.findByMatchIdOrderByOccurredAtAsc(matchId);
    }

    public boolean exists(UUID eventId) {
        return eventStoreRepository.existsByEventId(eventId);
    }

    public MatchEvent deserializeEvent(EventStoreEntry entry) {
        try {
            return objectMapper.readValue(entry.getPayload(), MatchEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize event: {}", entry.getEventId(), e);
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
}
