export interface Player {
  id: string; // nickname as id for now as per API
  name: string;
  isHost: boolean;
  score: number;
  colorIndex?: number; // 플레이어 슬롯 번호 (0~7), 색상 매핑용
}

export interface Room {
  id: string;
  name: string;
  hostName: string;
  playerCount: number;
  maxPlayers: number;
  status: string;
}

export type GameEvent =
  | { type: 'PLAYER_JOIN'; player: string }
  | { type: 'PLAYER_LEAVE'; player: string }
  | { type: 'HOST_CHANGE'; newHost: string }
  | { type: 'CHAT'; playerName: string; message: string }
  | { type: 'GAME_START'; gameType: string; category: string; songCount: number; message: string }
  | { type: 'ROUND_START'; videoURL: string; roundIndex: number; currentRound: number; totalRound: number }
  | { type: 'TIMER_TICK'; remainingSeconds: number }
  | { type: 'CORRECT_ANSWER'; playerName: string; answer: string; score: number }
  | { type: 'ROUND_END'; answer: string; winner: string }
  | { type: 'GAME_RESULT'; rankings: string };

export type GameStatus = 'LOBBY' | 'ROOM_LIST' | 'WAITING' | 'PLAYING' | 'RESULT';

export interface GameStartInfo {
  gameType: string;
  category: string;
  songCount: number;
  message: string;
}

export interface RoundEndInfo {
  answer: string;
  winner: string | null;
}
