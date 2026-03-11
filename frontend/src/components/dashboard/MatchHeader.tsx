import { useMatchStore } from '@/store/matchStore';
import { useEffect, useState } from 'react';

export function MatchHeader() {
  const status = useMatchStore((s) => s.simulatorStatus);
  const [clock, setClock] = useState(0);

  useEffect(() => {
    if (!status?.isRunning || status?.isPaused) return;
    const interval = setInterval(() => {
      setClock((c) => c + 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [status?.isRunning, status?.isPaused]);

  useEffect(() => {
    if (status?.currentMinute !== undefined) {
      setClock(status.currentMinute);
    }
  }, [status?.currentMinute]);

  const isLive = status?.isRunning && !status?.isPaused;
  const matchStatus = isLive ? 'LIVE' : status?.isPaused ? 'PAUSED' : status?.isRunning === false && clock > 0 ? 'FT' : 'WAITING';

  return (
    <div className="bg-zinc-900 border-b border-zinc-800 px-6 py-4">
      <div className="flex items-center justify-center gap-8">
        {/* Home Team */}
        <div className="text-right flex-1">
          <h2 className="text-2xl font-bold">{status?.homeTeam ?? 'Home Team'}</h2>
        </div>

        {/* Score */}
        <div className="flex items-center gap-4">
          <span className="text-5xl font-black tabular-nums">{status?.homeScore ?? 0}</span>
          <span className="text-2xl text-zinc-500">-</span>
          <span className="text-5xl font-black tabular-nums">{status?.awayScore ?? 0}</span>
        </div>

        {/* Away Team */}
        <div className="text-left flex-1">
          <h2 className="text-2xl font-bold">{status?.awayTeam ?? 'Away Team'}</h2>
        </div>
      </div>

      {/* Match Info Bar */}
      <div className="flex items-center justify-center gap-4 mt-2">
        {/* Status Badge */}
        <span className={`badge ${
          matchStatus === 'LIVE' ? 'bg-green-500/20 text-green-400' :
          matchStatus === 'PAUSED' ? 'bg-amber-500/20 text-amber-400' :
          matchStatus === 'FT' ? 'bg-red-500/20 text-red-400' :
          'bg-zinc-700/50 text-zinc-400'
        }`}>
          {matchStatus === 'LIVE' && (
            <span className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse mr-1.5" />
          )}
          {matchStatus}
        </span>

        {/* Clock */}
        <span className="text-lg font-mono text-zinc-300 tabular-nums">
          {clock}'
        </span>
      </div>
    </div>
  );
}
