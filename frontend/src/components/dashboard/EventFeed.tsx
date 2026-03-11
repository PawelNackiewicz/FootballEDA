import { useMatchStore } from '@/store/matchStore';
import type { MatchEvent, GoalScored, CardIssued, SubstitutionMade, MatchStatusChanged, FoulCommitted } from '@/types/events';

const eventIcons: Record<string, string> = {
  GOAL: '\u26BD',
  CARD: '\uD83D\uDFE8',
  SUBSTITUTION: '\uD83D\uDD04',
  STATUS_CHANGE: '\uD83D\uDCE2',
  FOUL: '\u26A0\uFE0F',
};

const eventColors: Record<string, string> = {
  GOAL: 'border-green-500 bg-green-500/10',
  CARD: 'border-amber-500 bg-amber-500/10',
  SUBSTITUTION: 'border-blue-500 bg-blue-500/10',
  STATUS_CHANGE: 'border-purple-500 bg-purple-500/10',
  FOUL: 'border-orange-500 bg-orange-500/10',
};

function getEventDescription(event: MatchEvent): string {
  switch (event.type) {
    case 'GOAL': {
      const g = event as GoalScored;
      return `GOAL! ${g.scoringPlayerName}${g.isOwnGoal ? ' (OG)' : ''} - ${g.homeScore}:${g.awayScore}`;
    }
    case 'CARD': {
      const c = event as CardIssued;
      return `${c.cardType} Card - ${c.playerName}`;
    }
    case 'SUBSTITUTION': {
      const s = event as SubstitutionMade;
      return `${s.playerOutName} -> ${s.playerInName}`;
    }
    case 'STATUS_CHANGE': {
      const m = event as MatchStatusChanged;
      return `${m.newStatus.replace('_', ' ')}`;
    }
    case 'FOUL': {
      const f = event as FoulCommitted;
      return `Foul by ${f.playerName} (${f.severity})`;
    }
    default:
      return 'Unknown event';
  }
}

export function EventFeed() {
  const events = useMatchStore((s) => s.events);

  return (
    <div className="card h-full flex flex-col">
      <h3 className="text-sm font-semibold text-zinc-400 uppercase tracking-wider mb-3">Live Events</h3>
      <div className="flex-1 overflow-y-auto space-y-2">
        {events.length === 0 ? (
          <p className="text-sm text-zinc-500 text-center py-8">Waiting for match events...</p>
        ) : (
          events.slice(0, 50).map((event) => (
            <div
              key={event.eventId}
              className={`flex items-start gap-3 p-2 rounded-lg border-l-2 animate-slide-in ${eventColors[event.type] ?? 'border-zinc-700'}`}
            >
              <span className="text-lg leading-none mt-0.5">{eventIcons[event.type] ?? '?'}</span>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="badge bg-zinc-700 text-zinc-300 text-[10px]">{event.matchMinute}'</span>
                  <span className="text-xs text-zinc-500 uppercase">{event.type.replace('_', ' ')}</span>
                </div>
                <p className="text-sm text-zinc-200 mt-0.5 truncate">{getEventDescription(event)}</p>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
