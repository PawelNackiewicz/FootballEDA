package com.footballeda.api;

import com.footballeda.domain.model.Match;
import com.footballeda.domain.model.Standing;
import com.footballeda.repository.MatchRepository;
import com.footballeda.service.MatchReplayService;
import com.footballeda.service.StandingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MatchController {

    private final MatchRepository matchRepository;
    private final MatchReplayService replayService;
    private final StandingsService standingsService;

    @GetMapping
    public ResponseEntity<List<Match>> listMatches() {
        return ResponseEntity.ok(matchRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Match> getMatch(@PathVariable UUID id) {
        return matchRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/replay")
    public ResponseEntity<Map<String, Object>> replay(@PathVariable UUID id) {
        return ResponseEntity.ok(replayService.replay(id));
    }

    @GetMapping("/standings")
    public ResponseEntity<List<Standing>> getStandings() {
        return ResponseEntity.ok(standingsService.getStandings());
    }
}
