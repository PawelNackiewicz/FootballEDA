import { useWebSocket } from '@/hooks/useWebSocket';
import { useMatchStore } from '@/store/matchStore';
import { MatchHeader } from '@/components/dashboard/MatchHeader';
import { EventFeed } from '@/components/dashboard/EventFeed';
import { StandingsTable } from '@/components/dashboard/StandingsTable';
import { StatisticsPanel } from '@/components/dashboard/StatisticsPanel';
import { NotificationBell } from '@/components/dashboard/NotificationBell';
import { SimulatorControls } from '@/components/simulator/SimulatorControls';
import { SpeedControl } from '@/components/simulator/SpeedControl';
import { EventFlowVisualizer } from '@/components/eda/EventFlowVisualizer';
import { ConsumerStatus } from '@/components/eda/ConsumerStatus';
import { EventStoreViewer } from '@/components/eda/EventStoreViewer';
import { useState } from 'react';

type Tab = 'stats' | 'standings' | 'events' | 'notifications';

function App() {
  const { isConnected } = useWebSocket();
  const goalFlash = useMatchStore((s) => s.goalFlash);
  const [activeTab, setActiveTab] = useState<Tab>('stats');

  return (
    <div className={`min-h-screen bg-zinc-950 text-zinc-100 transition-colors duration-500 ${goalFlash ? 'animate-goal-flash' : ''}`}>
      {/* Top Bar */}
      <header className="border-b border-zinc-800 px-6 py-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <h1 className="text-xl font-bold text-green-500">FootballEDA</h1>
            <div className="flex items-center gap-1.5">
              <div className={`w-2 h-2 rounded-full ${isConnected ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
              <span className="text-xs text-zinc-400">{isConnected ? 'CONNECTED' : 'DISCONNECTED'}</span>
            </div>
          </div>
          <NotificationBell />
        </div>
      </header>

      {/* Match Header */}
      <MatchHeader />

      {/* Main Content */}
      <div className="flex gap-4 p-4 h-[calc(100vh-200px)]">
        {/* Left: Event Feed */}
        <div className="w-80 flex-shrink-0">
          <EventFeed />
        </div>

        {/* Center: EDA Visualizer */}
        <div className="flex-1 flex flex-col gap-4">
          <div className="flex-1">
            <EventFlowVisualizer />
          </div>
          <ConsumerStatus />
        </div>

        {/* Right: Tabbed Panel */}
        <div className="w-96 flex-shrink-0 flex flex-col">
          <div className="flex border-b border-zinc-800 mb-3">
            {(['stats', 'standings', 'events', 'notifications'] as Tab[]).map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`px-4 py-2 text-sm font-medium capitalize transition-colors ${
                  activeTab === tab
                    ? 'text-green-500 border-b-2 border-green-500'
                    : 'text-zinc-400 hover:text-zinc-200'
                }`}
              >
                {tab}
              </button>
            ))}
          </div>
          <div className="flex-1 overflow-auto">
            {activeTab === 'stats' && <StatisticsPanel />}
            {activeTab === 'standings' && <StandingsTable />}
            {activeTab === 'events' && <EventStoreViewer />}
            {activeTab === 'notifications' && (
              <div className="space-y-2">
                {useMatchStore.getState().notifications.map((n) => (
                  <div key={n.id} className="card text-sm animate-slide-in">
                    <span className="text-zinc-400">{n.matchMinute}'</span>{' '}
                    <span>{n.message}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Bottom Bar */}
      <footer className="fixed bottom-0 left-0 right-0 bg-zinc-900 border-t border-zinc-800 px-6 py-3">
        <div className="flex items-center justify-between">
          <SimulatorControls />
          <SpeedControl />
        </div>
      </footer>
    </div>
  );
}

export default App;
