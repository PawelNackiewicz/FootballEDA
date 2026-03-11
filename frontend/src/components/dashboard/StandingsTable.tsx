import { useMatchStore } from '@/store/matchStore';

export function StandingsTable() {
  const standings = useMatchStore((s) => s.standings);

  const sorted = [...standings].sort((a, b) => {
    if (b.points !== a.points) return b.points - a.points;
    const gdA = a.goalsFor - a.goalsAgainst;
    const gdB = b.goalsFor - b.goalsAgainst;
    if (gdB !== gdA) return gdB - gdA;
    return b.goalsFor - a.goalsFor;
  });

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-zinc-400 text-xs uppercase border-b border-zinc-800">
            <th className="text-left py-2 px-2">#</th>
            <th className="text-left py-2 px-2">Team</th>
            <th className="text-center py-2 px-1">P</th>
            <th className="text-center py-2 px-1">W</th>
            <th className="text-center py-2 px-1">D</th>
            <th className="text-center py-2 px-1">L</th>
            <th className="text-center py-2 px-1">GF</th>
            <th className="text-center py-2 px-1">GA</th>
            <th className="text-center py-2 px-1">GD</th>
            <th className="text-center py-2 px-1 font-bold">Pts</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((s, i) => (
            <tr
              key={s.teamId}
              className="border-b border-zinc-800/50 hover:bg-zinc-800/30 transition-colors"
            >
              <td className="py-2 px-2 text-zinc-400">{i + 1}</td>
              <td className="py-2 px-2 font-medium">{s.teamName}</td>
              <td className="text-center py-2 px-1">{s.played}</td>
              <td className="text-center py-2 px-1">{s.won}</td>
              <td className="text-center py-2 px-1">{s.drawn}</td>
              <td className="text-center py-2 px-1">{s.lost}</td>
              <td className="text-center py-2 px-1">{s.goalsFor}</td>
              <td className="text-center py-2 px-1">{s.goalsAgainst}</td>
              <td className="text-center py-2 px-1">{s.goalsFor - s.goalsAgainst}</td>
              <td className="text-center py-2 px-1 font-bold text-green-400">{s.points}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {standings.length === 0 && (
        <p className="text-center text-zinc-500 text-sm py-8">No standings data yet</p>
      )}
    </div>
  );
}
