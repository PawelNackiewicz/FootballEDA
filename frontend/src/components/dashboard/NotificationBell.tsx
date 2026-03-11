import { useState } from 'react';
import { useMatchStore } from '@/store/matchStore';

export function NotificationBell() {
  const notifications = useMatchStore((s) => s.notifications);
  const [isOpen, setIsOpen] = useState(false);
  const [readCount, setReadCount] = useState(0);
  const unreadCount = Math.max(0, notifications.length - readCount);

  const toggleOpen = () => {
    setIsOpen(!isOpen);
    if (!isOpen) {
      setReadCount(notifications.length);
    }
  };

  return (
    <div className="relative">
      <button onClick={toggleOpen} className="relative p-2 hover:bg-zinc-800 rounded-lg transition-colors">
        <svg className="w-5 h-5 text-zinc-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-80 bg-zinc-900 border border-zinc-800 rounded-lg shadow-xl z-50 max-h-96 overflow-y-auto">
          <div className="p-3 border-b border-zinc-800">
            <h4 className="text-sm font-semibold">Notifications</h4>
          </div>
          {notifications.length === 0 ? (
            <p className="text-sm text-zinc-500 p-4 text-center">No notifications</p>
          ) : (
            notifications.slice(0, 20).map((n) => (
              <div key={n.id} className="p-3 border-b border-zinc-800/50 hover:bg-zinc-800/30 transition-colors">
                <p className="text-sm">{n.message}</p>
                <span className="text-xs text-zinc-500">{n.matchMinute}'</span>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
