export type RecordedGameType = 'SONG' | 'CS' | 'HANGMAN';

export interface GameRecord {
  gameType: RecordedGameType;
  played: number;
  won: number;
  lost: number;
  winRate: number | null;
}
