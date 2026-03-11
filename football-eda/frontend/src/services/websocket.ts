import { Client } from '@stomp/stompjs';
import type { MatchEvent, MatchStats, Standing, Notification } from '@/types/events';

export interface WebSocketCallbacks {
  onEvent: (event: MatchEvent) => void;
  onStandings: (standings: Standing[]) => void;
  onStats: (stats: MatchStats[]) => void;
  onNotification: (notification: Notification) => void;
  onConsumerHealth: (health: Record<string, unknown>) => void;
  onConnect: () => void;
  onDisconnect: () => void;
}

let client: Client | null = null;

export function connect(callbacks: WebSocketCallbacks): void {
  if (client?.active) return;

  client = new Client({
    brokerURL: `ws://${window.location.hostname}:8080/ws/websocket`,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      callbacks.onConnect();

      client?.subscribe('/topic/events', (message) => {
        callbacks.onEvent(JSON.parse(message.body));
      });

      client?.subscribe('/topic/standings', (message) => {
        callbacks.onStandings(JSON.parse(message.body));
      });

      client?.subscribe('/topic/stats', (message) => {
        callbacks.onStats(JSON.parse(message.body));
      });

      client?.subscribe('/topic/notifications', (message) => {
        callbacks.onNotification(JSON.parse(message.body));
      });

      client?.subscribe('/topic/consumers', (message) => {
        callbacks.onConsumerHealth(JSON.parse(message.body));
      });
    },
    onDisconnect: () => {
      callbacks.onDisconnect();
    },
    onStompError: (frame) => {
      console.error('STOMP error:', frame.headers['message']);
    },
  });

  client.activate();
}

export function disconnect(): void {
  client?.deactivate();
  client = null;
}
