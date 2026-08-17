export type ReportSource = 'IN_GAME' | 'LOBBY';

export type ReportReason = 'CONTENT_NOT_SHOWN' | 'CONTENT_WRONG' | 'HINT_WRONG' | 'ANSWER_WRONG' | 'ETC';

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
