// Union type: works like a lightweight Java Enum for allowed values
export type MatchEventType = 'SHOT' | 'GOAL' | 'PASS' | 'FOUL';

// Represents a clicked spot on the 2D pitch (0 to 100 on both axes)
export interface PitchCoordinates {
  x: number;
  y: number;
}

// Matches the exact JSON payload expected by POST /api/v1/events in match-service
export interface MatchEventRequest {
  matchId: string;
  team: string;
  playerName: string;
  eventType: MatchEventType;
  minute: number;
  x: number;
  y: number;
}
