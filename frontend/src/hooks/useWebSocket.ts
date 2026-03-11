import { useEffect } from 'react';
import { connect, disconnect } from '@/services/websocket';
import { useMatchStore } from '@/store/matchStore';
import type { MatchEvent } from '@/types/events';

export function useWebSocket() {
  const {
    addEvent,
    setStandings,
    setStats,
    addNotification,
    setConnected,
    setGoalFlash,
    isConnected,
  } = useMatchStore();

  useEffect(() => {
    connect({
      onEvent: (event: MatchEvent) => {
        addEvent(event);
        if (event.type === 'GOAL') {
          setGoalFlash(true);
          setTimeout(() => setGoalFlash(false), 2000);
        }
      },
      onStandings: setStandings,
      onStats: setStats,
      onNotification: addNotification,
      onConsumerHealth: () => {},
      onConnect: () => setConnected(true),
      onDisconnect: () => setConnected(false),
    });

    return () => disconnect();
  }, [addEvent, setStandings, setStats, addNotification, setConnected, setGoalFlash]);

  return { isConnected };
}
