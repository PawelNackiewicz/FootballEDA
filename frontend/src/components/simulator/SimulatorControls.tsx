import { useState } from 'react';
import { useMatchStore } from '@/store/matchStore';
import type { SimulatorStatus } from '@/types/events';

export function SimulatorControls() {
  const simulatorStatus = useMatchStore((s) => s.simulatorStatus);
  const setSimulatorStatus = useMatchStore((s) => s.setSimulatorStatus);
  const [loading, setLoading] = useState(false);

  const isRunning = simulatorStatus?.isRunning ?? false;
  const isPaused = simulatorStatus?.isPaused ?? false;

  async function doAction(url: string, method = 'POST') {
    setLoading(true);
    try {
      const res = await fetch(`/api/simulator/${url}`, { method });
      const data = await res.json();
      setSimulatorStatus(data as SimulatorStatus);
    } catch (err) {
      console.error('Simulator action failed:', err);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex items-center gap-3">
      <span className="text-xs text-zinc-500 uppercase tracking-wider mr-2">Simulator</span>

      {!isRunning ? (
        <button
          onClick={() => doAction('start')}
          disabled={loading}
          className="px-4 py-1.5 bg-green-600 hover:bg-green-700 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
        >
          Start
        </button>
      ) : (
        <>
          {isPaused ? (
            <button
              onClick={() => doAction('resume')}
              disabled={loading}
              className="px-4 py-1.5 bg-green-600 hover:bg-green-700 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
            >
              Resume
            </button>
          ) : (
            <button
              onClick={() => doAction('pause')}
              disabled={loading}
              className="px-4 py-1.5 bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
            >
              Pause
            </button>
          )}
          <button
            onClick={() => doAction('stop')}
            disabled={loading}
            className="px-4 py-1.5 bg-red-600 hover:bg-red-700 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
          >
            Stop
          </button>
        </>
      )}

      {simulatorStatus?.matchId && (
        <button
          onClick={() => doAction(`../matches/${simulatorStatus.matchId}/replay`)}
          disabled={loading || isRunning}
          className="px-4 py-1.5 bg-purple-600 hover:bg-purple-700 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
        >
          Replay
        </button>
      )}

      {loading && <span className="text-xs text-zinc-500">...</span>}
    </div>
  );
}
