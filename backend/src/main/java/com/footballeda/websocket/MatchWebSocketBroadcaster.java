package com.footballeda.websocket;

import com.footballeda.domain.event.MatchEvent;
import com.footballeda.domain.model.MatchStats;
import com.footballeda.domain.model.Standing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchWebSocketBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastEvent(MatchEvent event) {
        log.debug("Broadcasting event to /topic/events: {}", event.type());
        messagingTemplate.convertAndSend("/topic/events", event);
    }

    public void broadcastStandings(List<Standing> standings) {
        log.debug("Broadcasting standings update to /topic/standings");
        messagingTemplate.convertAndSend("/topic/standings", standings);
    }

    public void broadcastStats(List<MatchStats> stats) {
        log.debug("Broadcasting stats update to /topic/stats");
        messagingTemplate.convertAndSend("/topic/stats", stats);
    }

    public void broadcastNotification(Map<String, Object> notification) {
        log.debug("Broadcasting notification to /topic/notifications");
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    public void broadcastConsumerHealth(Map<String, Object> health) {
        messagingTemplate.convertAndSend("/topic/consumers", health);
    }
}
