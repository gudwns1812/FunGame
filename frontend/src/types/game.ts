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
  gameType: string;
  csDifficulty: string;
}

export interface CreateRoomInput {
  title: string;
  maxPlayers: number;
  category: string;
  totalRound: number;
  gameType: string;
  difficulty: number;
  csDifficulty: string;
}

export interface RoomSettings {
  title: string;
  gameType: string;
  maxPlayers: number;
  category: string | null;
  totalRound: number;
  difficulty: number;
  csDifficulty: string;
  hostMemberId: number;
  hostNickname: string;
}

/**
 * 방 상태가 바뀌는 이벤트는 무엇이 바뀌었는지가 아니라 바뀐 뒤의 방 전체를 싣고 온다.
 * 구독 완료와 스냅샷 조회 사이의 이벤트가 두 번 적용돼도 깨지지 않도록,
 * 받는 쪽은 자기가 그리고 있는 것보다 version 이 낮은 상태를 버린다.
 */
export interface RoomState {
  version: number;
  players: { memberId: number; nickname: string; isReady: boolean }[];
  hostMemberId: number;
  hostNickname: string;
}

export type GameEvent =
  | { type: 'PLAYER_JOIN'; memberId: number; nickname: string; room: RoomState }
  | { type: 'PLAYER_LEAVE'; memberId: number; nickname: string; room: RoomState }
  | { type: 'PLAYER_READY'; memberId: number; nickname: string; ready: boolean; room: RoomState }
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
