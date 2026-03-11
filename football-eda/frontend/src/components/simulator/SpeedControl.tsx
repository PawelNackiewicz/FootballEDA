import { useState, useCallback } from 'react';
import { useMatchStore } from '@/store/matchStore';
import type { SimulatorStatus } from '@/types/events';

export function SpeedControl() {
  const simulatorStatus = useMatchStore((s) => s.simulatorStatus);
  const setSimulatorStatus = useMatchStore((s) => s.setSimulatorStatus);
  const [speed, setSpeed] = useState(simulatorStatus?.speed ?? 1);
  const [debounceTimer, setDebounceTimer] = useState<ReturnType<typeof setTimeout> | null>(null);

  const updateSpeed = useCallback(
    (value: number) => {
      setSpeed(value);
      if (debounceTimer) clearTimeout(debounceTimer);
      const timer = setTimeout(async () => {
        try {
          const res = await fetch(`/api/simulator/speed/${value}`, { method: 'PUT' });
          const data = await res.json();
          setSimulatorStatus(data as SimulatorStatus);
        } catch (err) {
          console.error('Failed to set speed:', err);
        }
      }, 300);
      setDebounceTimer(timer);
    },
    [debounceTimer, setSimulatorStatus]
  );

  return (
    <div className="flex items-center gap-3">
      <span className="text-xs text-zinc-500 uppercase tracking-wider">Speed</span>
      <input
        type="range"
        min="1"
        max="20"
        value={speed}
        onChange={(e) => updateSpeed(Number(e.target.value))}
        className="w-32 accent-green-500"
      />
      <span className="text-sm font-mono text-green-400 w-8">{speed}x</span>
    </div>
  );
}
