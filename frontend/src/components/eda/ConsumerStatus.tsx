import { useConsumerStats } from '@/hooks/useConsumerStats';

export function ConsumerStatus() {
  const consumers = useConsumerStats();

  return (
    <div className="grid grid-cols-4 gap-3">
      {consumers.map((consumer) => (
        <div key={consumer.name} className="card flex items-center gap-3">
          <div className={`w-2.5 h-2.5 rounded-full flex-shrink-0 ${
            consumer.status === 'healthy' ? 'bg-green-500' :
            consumer.status === 'lagging' ? 'bg-amber-500' : 'bg-red-500'
          }`} />
          <div className="min-w-0">
            <p className="text-xs font-medium text-zinc-300 truncate">{consumer.name}</p>
            <p className="text-[10px] text-zinc-500">{consumer.eventsProcessed} events</p>
          </div>
        </div>
      ))}
    </div>
  );
}
