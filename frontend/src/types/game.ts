export interface Player {
  memberId: number;
  name: string;
  isHost: boolean;
  isReady: boolean;
  score: number;
  colorIndex?: number; // 플레이어 슬롯 번호 (0~7), 색상 매핑용
}

export interface Room {
  id: string;
  name: string;
  hostMemberId: number;
  hostName: string;
  playerCount: number;
  maxPlayers: number;
  status: string;
}

export interface RoomSettings {
  title: string;
  gameType: string;
  maxPlayers: number;
  category: string | null;
  totalRound: number;
  difficulty: number;
  hostMemberId: number;
  hostNickname: string;
}

export type GameEvent =
  | { type: 'PLAYER_JOIN'; memberId: number; nickname: string }
  | { type: 'PLAYER_LEAVE'; memberId: number; nickname: string }
  | { type: 'PLAYER_READY'; memberId: number; nickname: string; ready: boolean }
  | { type: 'CHAT'; memberId: number; nickname: string; message: string }
  | { type: 'GAME_START'; gameType: string; category: string; songCount: number; message: string }
  | { type: 'ROUND_START'; videoURL: string; roundIndex: number; currentRound: number; totalRound: number }
  | { type: 'TIMER_TICK'; remainingSeconds: number }
  | { type: 'ROUND_HINT'; hint: string }
  | { type: 'CORRECT_ANSWER'; memberId: number; nickname: string; answer: string; score: number }
  | {
      type: 'ROUND_END';
      answer: string;
      explanation?: string;
      winnerMemberId: number | null;
      winnerNickname: string | null;
    }
  | { type: 'GAME_RESULT'; rankings: RankingEntry[]; answer?: string; score?: number }
  | { type: 'HANGMAN_ACTION'; memberId: number; nickname: string; letter: string; result: string; status: string[] };

export interface RankingEntry {
  memberId: number | null;
  nickname: string;
  score: number;
}

export type GameStatus = 'LOBBY' | 'ROOM_LIST' | 'WAITING' | 'PLAYING' | 'RESULT';

export interface HangmanStatus {
  currentDisplay: string;
  wrongLetters: string[];
  remainingTries: number;
  currentTurnPlayer: string;
  currentTurnMemberId: number | null;
  isGameOver: boolean;
  isWin: boolean;
}

export interface GameStartInfo {
  gameType: string;
  category: string;
  songCount: number;
  message: string;
}

export interface RoundEndInfo {
  answer: string;
  explanation?: string | null;
  winner: string | null;
}
