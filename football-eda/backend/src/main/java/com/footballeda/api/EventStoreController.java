package com.footballeda.api;

import com.footballeda.domain.model.EventStoreEntry;
import com.footballeda.service.EventStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EventStoreController {

    private final EventStoreService eventStoreService;

    @GetMapping("/{matchId}")
    public ResponseEntity<List<EventStoreEntry>> getEvents(@PathVariable UUID matchId) {
        return ResponseEntity.ok(eventStoreService.getEvents(matchId));
    }

    @GetMapping("/{matchId}/count")
    public ResponseEntity<Map<String, Object>> getEventCount(@PathVariable UUID matchId) {
        List<EventStoreEntry> events = eventStoreService.getEvents(matchId);
        return ResponseEntity.ok(Map.of("matchId", matchId, "count", events.size()));
    }

    @GetMapping("/types")
    public ResponseEntity<Map<String, Long>> getEventTypeBreakdown(@RequestParam UUID matchId) {
        List<EventStoreEntry> events = eventStoreService.getEvents(matchId);
        Map<String, Long> breakdown = events.stream()
                .collect(Collectors.groupingBy(EventStoreEntry::getEventType, Collectors.counting()));
        return ResponseEntity.ok(breakdown);
    }
}
