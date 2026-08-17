import type { ReportReason } from '../types/report';

interface ReasonChoice {
  reason: ReportReason;
  label: string;
}

const SONG_REASONS: ReasonChoice[] = [
  { reason: 'CONTENT_NOT_SHOWN', label: '노래가 안 나와요' },
  { reason: 'CONTENT_WRONG', label: '노래가 달라요' },
  { reason: 'HINT_WRONG', label: '초성 힌트가 이상해요' },
  { reason: 'ANSWER_WRONG', label: '답이 이상해요' },
];

const CS_REASONS: ReasonChoice[] = [
  { reason: 'CONTENT_NOT_SHOWN', label: '문제가 안 보여요' },
  { reason: 'CONTENT_WRONG', label: '문제가 이상해요' },
  { reason: 'ANSWER_WRONG', label: '답이 이상해요' },
];

const HANGMAN_REASONS: ReasonChoice[] = [
  { reason: 'CONTENT_NOT_SHOWN', label: '단어가 안 보여요' },
  { reason: 'CONTENT_WRONG', label: '단어가 이상해요' },
  { reason: 'HINT_WRONG', label: '틀린 글자 표시가 이상해요' },
  { reason: 'ANSWER_WRONG', label: '답이 이상해요' },
];

export const ETC_REASON: ReasonChoice = { reason: 'ETC', label: '기타 (직접 작성)' };

export const reasonChoicesFor = (gameType: string | null): ReasonChoice[] => {
  switch (gameType) {
    case 'CS':
      return [...CS_REASONS, ETC_REASON];
    case 'HANGMAN':
      return [...HANGMAN_REASONS, ETC_REASON];
    default:
      return [...SONG_REASONS, ETC_REASON];
  }
};

export const REPORTABLE_GAME_TYPES = [
  { gameType: 'SONG', label: '노래 퀴즈' },
  { gameType: 'CS', label: 'CS 퀴즈' },
  { gameType: 'HANGMAN', label: '행맨' },
];
