import { useMatchStore } from '@/store/matchStore';

interface StatBarProps {
  label: string;
  homeValue: number;
  awayValue: number;
  format?: (v: number) => string;
}

function StatBar({ label, homeValue, awayValue, format }: StatBarProps) {
  const total = homeValue + awayValue || 1;
  const homePercent = (homeValue / total) * 100;

  const display = format ?? ((v: number) => String(v));

  return (
    <div className="space-y-1">
      <div className="flex justify-between text-sm">
        <span className="text-green-400 font-medium">{display(homeValue)}</span>
        <span className="text-zinc-400 text-xs uppercase">{label}</span>
        <span className="text-blue-400 font-medium">{display(awayValue)}</span>
      </div>
      <div className="flex h-2 rounded-full overflow-hidden bg-zinc-800">
        <div
          className="bg-green-500 transition-all duration-500 ease-out"
          style={{ width: `${homePercent}%` }}
        />
        <div
          className="bg-blue-500 transition-all duration-500 ease-out"
          style={{ width: `${100 - homePercent}%` }}
        />
      </div>
    </div>
  );
}

export function StatisticsPanel() {
  const stats = useMatchStore((s) => s.stats);

  if (stats.length < 2) {
    return <p className="text-center text-zinc-500 text-sm py-8">Waiting for match stats...</p>;
  }

  const home = stats[0];
  const away = stats[1];

  return (
    <div className="space-y-4">
      <StatBar label="Possession" homeValue={home.possession} awayValue={away.possession} format={(v) => `${v.toFixed(1)}%`} />
      <StatBar label="Shots" homeValue={home.shots} awayValue={away.shots} />
      <StatBar label="Shots on Target" homeValue={home.shotsOnTarget} awayValue={away.shotsOnTarget} />
      <StatBar label="Fouls" homeValue={home.fouls} awayValue={away.fouls} />
      <StatBar label="Corners" homeValue={home.corners} awayValue={away.corners} />
      <StatBar label="Yellow Cards" homeValue={home.yellowCards} awayValue={away.yellowCards} />
      <StatBar label="Red Cards" homeValue={home.redCards} awayValue={away.redCards} />
      <StatBar label="Expected Goals (xG)" homeValue={home.expectedGoals} awayValue={away.expectedGoals} format={(v) => v.toFixed(2)} />
    </div>
  );
}
