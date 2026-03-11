import { useMatchStore } from '@/store/matchStore';
import { useState } from 'react';
import type { EventType } from '@/types/events';

export function EventStoreViewer() {
  const events = useMatchStore((s) => s.events);
  const [filter, setFilter] = useState<EventType | 'ALL'>('ALL');

  const filtered = filter === 'ALL' ? events : events.filter((e) => e.type === filter);

  return (
    <div className="space-y-3">
      {/* Filter */}
      <div className="flex gap-2 flex-wrap">
        {(['ALL', 'GOAL', 'CARD', 'SUBSTITUTION', 'STATUS_CHANGE', 'FOUL'] as const).map((type) => (
          <button
            key={type}
            onClick={() => setFilter(type)}
            className={`px-2 py-0.5 text-[10px] rounded-full uppercase transition-colors ${
              filter === type
                ? 'bg-green-500/20 text-green-400 border border-green-500/50'
                : 'bg-zinc-800 text-zinc-400 border border-zinc-700 hover:text-zinc-200'
            }`}
          >
            {type}
          </button>
        ))}
      </div>

      {/* Event list */}
      <div className="space-y-1 max-h-96 overflow-y-auto">
        {filtered.length === 0 ? (
          <p className="text-sm text-zinc-500 text-center py-4">No events</p>
        ) : (
          filtered.map((event, i) => (
            <div key={event.eventId} className="flex items-center gap-2 text-xs py-1.5 border-b border-zinc-800/50">
              <span className="text-zinc-500 w-6 text-right">{filtered.length - i}</span>
              <span className="font-mono text-zinc-600 w-16 truncate">{event.eventId.slice(0, 8)}</span>
              <span className={`badge text-[9px] ${
                event.type === 'GOAL' ? 'bg-green-500/20 text-green-400' :
                event.type === 'CARD' ? 'bg-amber-500/20 text-amber-400' :
                event.type === 'SUBSTITUTION' ? 'bg-blue-500/20 text-blue-400' :
                event.type === 'STATUS_CHANGE' ? 'bg-purple-500/20 text-purple-400' :
                'bg-orange-500/20 text-orange-400'
              }`}>
                {event.type}
              </span>
              <span className="text-zinc-400">{event.matchMinute}'</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
