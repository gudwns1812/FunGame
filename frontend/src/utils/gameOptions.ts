export const GAME_TYPES = [
  { value: 'SONG', label: '음악 퀴즈' },
  { value: 'CS', label: 'CS 퀴즈' },
  { value: 'HANGMAN', label: '행맨' },
];

export const CATEGORIES = [
  { value: 'TOTAL', label: '전체' },
  { value: 'KPOP', label: 'K-POP' },
  { value: 'POP', label: 'POP' },
  { value: 'BALLAD', label: '발라드' },
  { value: 'RAP', label: '랩/힙합' },
  { value: 'OST', label: 'OST' },
];

const SONG_COUNT_OPTIONS = [10, 20, 30, 40, 50, 60, 70, 80, 90, 100];

export const isSingleRound = (gameType: string) => gameType === 'HANGMAN';

export const roundOptionsFor = (gameType: string) => {
  if (gameType === 'HANGMAN') return [1];
  if (gameType === 'CS') return SONG_COUNT_OPTIONS.filter((count) => count <= 50);
  return SONG_COUNT_OPTIONS;
};

export const maxPlayerOptionsFor = (gameType: string) =>
  gameType === 'HANGMAN' ? [2, 3, 4, 5, 6] : [2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

export const capacityOf = (gameType: string) => Math.max(...maxPlayerOptionsFor(gameType));

export const canHold = (gameType: string, playerCount: number) => capacityOf(gameType) >= playerCount;

export interface GameOptionValues {
  category: string;
  totalRound: number;
  maxPlayers: number;
}

export const applyGameTypeConstraints = (
  gameType: string,
  values: GameOptionValues,
  minMaxPlayers = 2,
): GameOptionValues => {
  const rounds = roundOptionsFor(gameType);
  const players = maxPlayerOptionsFor(gameType).filter((count) => count >= minMaxPlayers);
  const fallbackPlayers = players[players.length - 1] ?? capacityOf(gameType);

  return {
    category: gameType === 'SONG' ? (values.category === 'DEFAULT' ? 'KPOP' : values.category) : 'DEFAULT',
    totalRound: rounds.includes(values.totalRound) ? values.totalRound : rounds[rounds.length - 1],
    maxPlayers: players.includes(values.maxPlayers) ? values.maxPlayers : fallbackPlayers,
  };
};
