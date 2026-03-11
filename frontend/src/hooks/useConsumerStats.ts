import { useMemo } from 'react';
import { useMatchStore } from '@/store/matchStore';
import type { ConsumerHealth } from '@/types/events';

export function useConsumerStats(): ConsumerHealth[] {
  const events = useMatchStore((s) => s.events);

  return useMemo(() => {
    const consumers: ConsumerHealth[] = [
      { name: 'StandingsConsumer', eventsProcessed: 0, status: 'healthy', lastProcessedAt: null },
      { name: 'StatsConsumer', eventsProcessed: 0, status: 'healthy', lastProcessedAt: null },
      { name: 'NotificationConsumer', eventsProcessed: 0, status: 'healthy', lastProcessedAt: null },
      { name: 'AuditConsumer', eventsProcessed: 0, status: 'healthy', lastProcessedAt: null },
    ];

    const eventCount = events.length;
    consumers.forEach((c) => {
      c.eventsProcessed = eventCount;
      c.lastProcessedAt = events[0]?.occurredAt ?? null;
    });

    return consumers;
  }, [events]);
}
