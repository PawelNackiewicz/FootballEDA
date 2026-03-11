export type EventType = 'GOAL' | 'CARD' | 'SUBSTITUTION' | 'STATUS_CHANGE' | 'FOUL';
export type CardType = 'YELLOW' | 'RED';
export type MatchStatus = 'NOT_STARTED' | 'FIRST_HALF' | 'HALF_TIME' | 'SECOND_HALF' | 'FULL_TIME';

export interface MatchEvent {
  eventId: string;
  matchId: string;
  occurredAt: string;
  matchMinute: number;
  type: EventType;
}

export interface GoalScored extends MatchEvent {
  type: 'GOAL';
  scoringPlayerId: string;
  scoringPlayerName: string;
  assistPlayerId: string | null;
  assistPlayerName: string | null;
  teamId: string;
  homeScore: number;
  awayScore: number;
  isOwnGoal: boolean;
}

export interface CardIssued extends MatchEvent {
  type: 'CARD';
  playerId: string;
  playerName: string;
  teamId: string;
  cardType: CardType;
  reason: string;
}

export interface SubstitutionMade extends MatchEvent {
  type: 'SUBSTITUTION';
  playerOutId: string;
  playerOutName: string;
  playerInId: string;
  playerInName: string;
  teamId: string;
}

export interface MatchStatusChanged extends MatchEvent {
  type: 'STATUS_CHANGE';
  oldStatus: MatchStatus;
  newStatus: MatchStatus;
}

export interface FoulCommitted extends MatchEvent {
  type: 'FOUL';
  playerId: string;
  playerName: string;
  teamId: string;
  severity: string;
}

export interface MatchStats {
  id: string;
  matchId: string;
  teamId: string;
  possession: number;
  shots: number;
  shotsOnTarget: number;
  fouls: number;
  corners: number;
  yellowCards: number;
  redCards: number;
  expectedGoals: number;
}

export interface Standing {
  id: string;
  teamId: string;
  teamName: string;
  played: number;
  won: number;
  drawn: number;
  lost: number;
  goalsFor: number;
  goalsAgainst: number;
  points: number;
}

export interface Notification {
  id: string;
  message: string;
  type: EventType;
  matchMinute: number;
  timestamp: string;
}

export interface SimulatorStatus {
  isRunning: boolean;
  isPaused: boolean;
  currentMinute: number;
  speed: number;
  matchId: string | null;
  homeTeam: string | null;
  awayTeam: string | null;
  homeScore: number;
  awayScore: number;
}

export interface ConsumerHealth {
  name: string;
  eventsProcessed: number;
  status: 'healthy' | 'lagging' | 'failed';
  lastProcessedAt: string | null;
}
