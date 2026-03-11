import { useMemo } from 'react';
import { useMatchStore } from '@/store/matchStore';
import type { GoalScored } from '@/types/events';

export function useMatchEvents() {
  const events = useMatchStore((s) => s.events);
  const simulatorStatus = useMatchStore((s) => s.simulatorStatus);

  const lastGoal = useMemo(() => {
    return events.find((e) => e.type === 'GOAL') as GoalScored | undefined;
  }, [events]);

  const currentScore = useMemo(() => {
    if (simulatorStatus) {
      return { home: simulatorStatus.homeScore, away: simulatorStatus.awayScore };
    }
    if (lastGoal) {
      return { home: lastGoal.homeScore, away: lastGoal.awayScore };
    }
    return { home: 0, away: 0 };
  }, [simulatorStatus, lastGoal]);

  const matchMinute = simulatorStatus?.currentMinute ?? 0;

  return { events, lastGoal, currentScore, matchMinute };
}
