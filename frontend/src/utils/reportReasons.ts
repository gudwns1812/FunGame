import type { ReportReason, ReportSource, ReportStatus } from '../types/report';

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

const REASON_LABELS: Record<ReportReason, string> = {
  CONTENT_NOT_SHOWN: '문제가 나오지 않음',
  CONTENT_WRONG: '문제가 이상함',
  HINT_WRONG: '힌트가 이상함',
  ANSWER_WRONG: '답이 이상함',
  ETC: '기타 문의',
};

const STATUS_LABELS: Record<ReportStatus, string> = {
  OPEN: '접수됨',
  RESOLVED: '처리 완료',
};

const SOURCE_LABELS: Record<ReportSource, string> = {
  IN_GAME: '게임 중',
  LOBBY: '로비',
};

export const reasonLabelOf = (reason: ReportReason) => REASON_LABELS[reason];

export const statusLabelOf = (status: ReportStatus) => STATUS_LABELS[status];

export const sourceLabelOf = (source: ReportSource) => SOURCE_LABELS[source];

export const gameTypeLabelOf = (gameType: string | null) =>
  REPORTABLE_GAME_TYPES.find((option) => option.gameType === gameType)?.label ?? '게임 밖';
