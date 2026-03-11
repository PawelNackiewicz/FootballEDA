import { useEffect, useState, useRef } from 'react';
import { useMatchStore } from '@/store/matchStore';

const eventTypeColors: Record<string, string> = {
  GOAL: '#22c55e',
  CARD: '#eab308',
  SUBSTITUTION: '#3b82f6',
  STATUS_CHANGE: '#a855f7',
  FOUL: '#f97316',
};

interface AnimatedDot {
  id: string;
  color: string;
  phase: number; // 0=producer->exchange, 1=exchange->queues, 2=done
  startTime: number;
  targetQueues: number[];
}

const QUEUE_NAMES = ['standings', 'stats', 'notifications', 'audit'];

function getTargetQueues(type: string): number[] {
  switch (type) {
    case 'GOAL': return [0, 1, 2, 3];
    case 'CARD': return [1, 2, 3];
    case 'SUBSTITUTION': return [1, 2, 3];
    case 'STATUS_CHANGE': return [0, 1, 2, 3];
    case 'FOUL': return [1, 3];
    default: return [3];
  }
}

export function EventFlowVisualizer() {
  const events = useMatchStore((s) => s.events);
  const simulatorStatus = useMatchStore((s) => s.simulatorStatus);
  const [dots, setDots] = useState<AnimatedDot[]>([]);
  const lastEventRef = useRef<string | null>(null);
  const svgRef = useRef<SVGSVGElement>(null);

  useEffect(() => {
    if (events.length === 0) return;
    const latest = events[0];
    if (latest.eventId === lastEventRef.current) return;
    lastEventRef.current = latest.eventId;

    const newDot: AnimatedDot = {
      id: latest.eventId,
      color: eventTypeColors[latest.type] ?? '#888',
      phase: 0,
      startTime: Date.now(),
      targetQueues: getTargetQueues(latest.type),
    };

    setDots((prev) => [...prev.slice(-10), newDot]);

    // Animate phases
    setTimeout(() => {
      setDots((prev) => prev.map((d) => d.id === newDot.id ? { ...d, phase: 1 } : d));
    }, 400);
    setTimeout(() => {
      setDots((prev) => prev.map((d) => d.id === newDot.id ? { ...d, phase: 2 } : d));
    }, 900);
    setTimeout(() => {
      setDots((prev) => prev.filter((d) => d.id !== newDot.id));
    }, 1500);
  }, [events]);

  const isActive = simulatorStatus?.isRunning && !simulatorStatus?.isPaused;

  return (
    <div className="card h-full flex flex-col">
      <h3 className="text-sm font-semibold text-zinc-400 uppercase tracking-wider mb-3">
        EDA Architecture Flow
      </h3>
      <div className="flex-1 relative">
        <svg ref={svgRef} viewBox="0 0 800 300" className="w-full h-full" preserveAspectRatio="xMidYMid meet">
          {/* Producer */}
          <rect x="20" y="110" width="140" height="60" rx="8" fill="#18181b" stroke={isActive ? '#22c55e' : '#3f3f46'} strokeWidth="2" />
          <text x="90" y="135" textAnchor="middle" fill="#a1a1aa" fontSize="11">Match</text>
          <text x="90" y="152" textAnchor="middle" fill="#e4e4e7" fontSize="13" fontWeight="bold">Simulator</text>
          {isActive && <circle cx="150" cy="118" r="4" fill="#22c55e"><animate attributeName="opacity" values="1;0.3;1" dur="1.5s" repeatCount="indefinite" /></circle>}

          {/* Arrow: Producer -> Exchange */}
          <line x1="160" y1="140" x2="250" y2="140" stroke="#3f3f46" strokeWidth="2" markerEnd="url(#arrowhead)" />

          {/* Exchange */}
          <rect x="250" y="100" width="160" height="80" rx="8" fill="#18181b" stroke="#a855f7" strokeWidth="2" />
          <text x="330" y="130" textAnchor="middle" fill="#a1a1aa" fontSize="10">Topic Exchange</text>
          <text x="330" y="150" textAnchor="middle" fill="#c084fc" fontSize="13" fontWeight="bold">football.events</text>
          <text x="330" y="168" textAnchor="middle" fill="#71717a" fontSize="9">fan-out routing</text>

          {/* Queue boxes and connections */}
          {QUEUE_NAMES.map((name, i) => {
            const queueY = 30 + i * 65;
            const queueX = 520;
            const consumerX = 660;
            return (
              <g key={name}>
                {/* Line from exchange to queue */}
                <line x1="410" y1="140" x2={queueX} y2={queueY + 20} stroke="#3f3f46" strokeWidth="1.5" />
                {/* Queue */}
                <rect x={queueX} y={queueY} width="120" height="40" rx="6" fill="#18181b" stroke="#3b82f6" strokeWidth="1.5" />
                <text x={queueX + 60} y={queueY + 16} textAnchor="middle" fill="#71717a" fontSize="9">{name}.queue</text>
                <text x={queueX + 60} y={queueY + 30} textAnchor="middle" fill="#e4e4e7" fontSize="10" fontWeight="500">
                  {events.length} msgs
                </text>
                {/* Arrow to consumer */}
                <line x1={queueX + 120} y1={queueY + 20} x2={consumerX} y2={queueY + 20} stroke="#3f3f46" strokeWidth="1.5" markerEnd="url(#arrowhead)" />
                {/* Consumer */}
                <rect x={consumerX} y={queueY} width="120" height="40" rx="6" fill="#18181b" stroke="#22c55e" strokeWidth="1.5" />
                <circle cx={consumerX + 12} cy={queueY + 20} r="4" fill="#22c55e" />
                <text x={consumerX + 70} y={queueY + 24} textAnchor="middle" fill="#e4e4e7" fontSize="10">
                  {name.charAt(0).toUpperCase() + name.slice(1)}
                </text>
              </g>
            );
          })}

          {/* Animated dots */}
          {dots.map((dot) => {
            if (dot.phase === 0) {
              return (
                <circle key={dot.id} r="5" fill={dot.color} opacity="0.9">
                  <animate attributeName="cx" from="160" to="250" dur="0.4s" fill="freeze" />
                  <animate attributeName="cy" from="140" to="140" dur="0.4s" fill="freeze" />
                </circle>
              );
            }
            if (dot.phase === 1) {
              return dot.targetQueues.map((qi) => (
                <circle key={`${dot.id}-${qi}`} r="4" fill={dot.color} opacity="0.8">
                  <animate attributeName="cx" from="410" to="520" dur="0.5s" fill="freeze" />
                  <animate attributeName="cy" from="140" to={30 + qi * 65 + 20} dur="0.5s" fill="freeze" />
                </circle>
              ));
            }
            if (dot.phase === 2) {
              return dot.targetQueues.map((qi) => (
                <circle key={`${dot.id}-c-${qi}`} r="3" fill={dot.color} opacity="0.6">
                  <animate attributeName="cx" from="640" to="720" dur="0.5s" fill="freeze" />
                  <animate attributeName="cy" values={`${50 + qi * 65};${50 + qi * 65}`} dur="0.5s" fill="freeze" />
                  <animate attributeName="opacity" from="0.6" to="0" dur="0.5s" fill="freeze" />
                </circle>
              ));
            }
            return null;
          })}

          {/* Arrow marker definition */}
          <defs>
            <marker id="arrowhead" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
              <polygon points="0 0, 8 3, 0 6" fill="#3f3f46" />
            </marker>
          </defs>
        </svg>

        {/* Legend */}
        <div className="absolute bottom-2 left-2 flex gap-3">
          {Object.entries(eventTypeColors).map(([type, color]) => (
            <div key={type} className="flex items-center gap-1">
              <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: color }} />
              <span className="text-[10px] text-zinc-500">{type}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
