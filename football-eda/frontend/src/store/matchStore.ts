import { create } from 'zustand';
import type { MatchEvent, MatchStats, Standing, Notification, SimulatorStatus } from '@/types/events';

interface MatchStore {
  events: MatchEvent[];
  standings: Standing[];
  stats: MatchStats[];
  notifications: Notification[];
  simulatorStatus: SimulatorStatus | null;
  isConnected: boolean;
  goalFlash: boolean;

  addEvent: (event: MatchEvent) => void;
  setStandings: (standings: Standing[]) => void;
  setStats: (stats: MatchStats[]) => void;
  addNotification: (notification: Notification) => void;
  setSimulatorStatus: (status: SimulatorStatus) => void;
  setConnected: (connected: boolean) => void;
  setGoalFlash: (flash: boolean) => void;
  clearAll: () => void;
}

export const useMatchStore = create<MatchStore>((set) => ({
  events: [],
  standings: [],
  stats: [],
  notifications: [],
  simulatorStatus: null,
  isConnected: false,
  goalFlash: false,

  addEvent: (event) =>
    set((state) => ({
      events: [event, ...state.events].slice(0, 200),
    })),

  setStandings: (standings) => set({ standings }),

  setStats: (stats) => set({ stats }),

  addNotification: (notification) =>
    set((state) => ({
      notifications: [notification, ...state.notifications].slice(0, 100),
    })),

  setSimulatorStatus: (simulatorStatus) => set({ simulatorStatus }),

  setConnected: (isConnected) => set({ isConnected }),

  setGoalFlash: (goalFlash) => set({ goalFlash }),

  clearAll: () =>
    set({
      events: [],
      standings: [],
      stats: [],
      notifications: [],
      simulatorStatus: null,
    }),
}));
