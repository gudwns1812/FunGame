export type ReportSource = 'IN_GAME' | 'LOBBY';

export type ReportReason = 'CONTENT_NOT_SHOWN' | 'CONTENT_WRONG' | 'HINT_WRONG' | 'ANSWER_WRONG' | 'ETC';

export type ReportStatus = 'OPEN' | 'RESOLVED';

export interface ReportPayload {
  source: ReportSource;
  roomId: number | null;
  reason: ReportReason;
  detail: string | null;
  gameType: string | null;
}

export interface ReportResult {
  ok: boolean;
  message: string | null;
}

export interface ReportCommentView {
  id: number;
  authorNickname: string;
  content: string;
  createdAt: string;
}

export interface MyReport {
  id: number;
  source: ReportSource;
  reason: ReportReason;
  detail: string | null;
  gameType: string | null;
  status: ReportStatus;
  createdAt: string;
  comments: ReportCommentView[];
}

export interface AdminReport extends MyReport {
  memberId: number;
  reporterNickname: string;
  quizCategory: string | null;
  contentId: number | null;
  roomId: number | null;
  currentRound: number | null;
  totalRound: number | null;
  quizContent: string | null;
  quizAnswer: string | null;
  quizHint: string | null;
}
