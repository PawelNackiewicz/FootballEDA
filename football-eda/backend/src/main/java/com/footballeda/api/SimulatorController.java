package com.footballeda.api;

import com.footballeda.producer.MatchSimulator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/simulator")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SimulatorController {

    private final MatchSimulator simulator;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(@RequestParam(required = false) UUID matchId) {
        if (matchId != null) {
            simulator.startMatch(matchId);
        } else {
            simulator.startNewMatch();
        }
        return ResponseEntity.ok(simulator.getStatus());
    }

    @PostMapping("/pause")
    public ResponseEntity<Map<String, Object>> pause() {
        simulator.pauseMatch();
        return ResponseEntity.ok(simulator.getStatus());
    }

    @PostMapping("/resume")
    public ResponseEntity<Map<String, Object>> resume() {
        simulator.resumeMatch();
        return ResponseEntity.ok(simulator.getStatus());
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        simulator.stopMatch();
        return ResponseEntity.ok(simulator.getStatus());
    }

    @PutMapping("/speed/{multiplier}")
    public ResponseEntity<Map<String, Object>> setSpeed(@PathVariable int multiplier) {
        simulator.setSpeed(multiplier);
        return ResponseEntity.ok(simulator.getStatus());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(simulator.getStatus());
    }
}
