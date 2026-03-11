const BASE_URL = '/api';

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${url}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  return response.json();
}

export const api = {
  startSimulation: (matchId?: string) =>
    request(`/simulator/start${matchId ? `?matchId=${matchId}` : ''}`, { method: 'POST' }),

  pauseSimulation: () =>
    request('/simulator/pause', { method: 'POST' }),

  resumeSimulation: () =>
    request('/simulator/resume', { method: 'POST' }),

  stopSimulation: () =>
    request('/simulator/stop', { method: 'POST' }),

  setSpeed: (multiplier: number) =>
    request(`/simulator/speed/${multiplier}`, { method: 'PUT' }),

  getStatus: () =>
    request<Record<string, unknown>>('/simulator/status'),

  replayMatch: (matchId: string) =>
    request(`/matches/${matchId}/replay`, { method: 'POST' }),

  getEvents: (matchId: string) =>
    request(`/events/${matchId}`),

  getStandings: () =>
    request('/matches/standings'),
};
