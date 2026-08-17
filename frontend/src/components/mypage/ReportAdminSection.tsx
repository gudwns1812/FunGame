import React, { useState } from 'react';
import { ALL_STATUSES, useReportAdmin } from '../../hooks/useReportAdmin';
import type { StatusFilter } from '../../hooks/useReportAdmin';
import { gameTypeLabelOf, reasonLabelOf, sourceLabelOf, statusLabelOf } from '../../utils/reportReasons';
import type { AdminReport } from '../../types/report';

const STATUS_FILTERS: { value: StatusFilter; label: string }[] = [
  { value: 'OPEN', label: '접수됨' },
  { value: 'RESOLVED', label: '처리 완료' },
  { value: ALL_STATUSES, label: '전체' },
];

interface ReportCardProps {
  report: AdminReport;
  onComment: (reportId: number, content: string) => void;
  onChangeStatus: (reportId: number, status: 'OPEN' | 'RESOLVED') => void;
}

const ContextLine: React.FC<{ name: string; value: string | number | null }> = ({ name, value }) => {
  if (value === null || value === '') {
    return null;
  }

  return (
    <div className="flex gap-2">
      <span className="px-label shrink-0 w-20">{name}</span>
      <span className="text-xs break-all">{value}</span>
    </div>
  );
};

const ReportCard: React.FC<ReportCardProps> = ({ report, onComment, onChangeStatus }) => {
  const [answer, setAnswer] = useState('');

  const submitAnswer = () => {
    if (!answer.trim()) {
      return;
    }

    onComment(report.id, answer.trim());
    setAnswer('');
  };

  return (
    <div className="px-card-sm p-3 space-y-2.5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2 min-w-0">
          <span className="px-chip num shrink-0">#{report.id}</span>
          <span className="px-title text-sm truncate">{reasonLabelOf(report.reason)}</span>
          <span className="px-label truncate">{report.reporterNickname}</span>
        </div>

        <span className={`px-chip ${report.status === 'RESOLVED' ? 'px-chip-grass' : 'px-chip-gold'}`}>
          {statusLabelOf(report.status)}
        </span>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <span className="px-chip px-chip-sea">{gameTypeLabelOf(report.gameType)}</span>
        <span className="px-chip">{sourceLabelOf(report.source)}</span>
        <span className="px-label">{new Date(report.createdAt).toLocaleString()}</span>
      </div>

      <div className="px-inset px-3 py-2 space-y-1">
        <ContextLine name="카테고리" value={report.quizCategory} />
        <ContextLine name="방" value={report.roomId} />
        <ContextLine
          name="라운드"
          value={report.currentRound === null ? null : `${report.currentRound} / ${report.totalRound}`}
        />
        <ContextLine name="문제 식별자" value={report.contentId} />
        <ContextLine name="문제" value={report.quizContent} />
        <ContextLine name="정답" value={report.quizAnswer} />
        <ContextLine name="힌트" value={report.quizHint} />
        <ContextLine name="직접 작성" value={report.detail} />
      </div>

      {report.comments.length > 0 && (
        <div className="space-y-2">
          {report.comments.map((comment) => (
            <div key={comment.id} className="px-inset px-3 py-2 space-y-1">
              <p className="px-label text-[10px]">
                {comment.authorNickname} · {new Date(comment.createdAt).toLocaleString()}
              </p>
              <p className="text-xs leading-relaxed">{comment.content}</p>
            </div>
          ))}
        </div>
      )}

      <div className="space-y-2">
        <label className="px-label block" htmlFor={`report-answer-${report.id}`}>
          답변 — 신고자에게 그대로 보입니다. 정답을 옮겨 적지 마세요.
        </label>
        <textarea
          id={`report-answer-${report.id}`}
          className="px-input w-full h-20"
          value={answer}
          onChange={(event) => setAnswer(event.target.value)}
        />

        <div className="flex flex-wrap gap-2">
          <button type="button" className="px-btn px-btn-sm px-btn-sea" onClick={submitAnswer}>
            답변 남기기
          </button>

          {report.status === 'OPEN' ? (
            <button
              type="button"
              className="px-btn px-btn-sm px-btn-grass"
              onClick={() => onChangeStatus(report.id, 'RESOLVED')}>
              처리 완료로
            </button>
          ) : (
            <button
              type="button"
              className="px-btn px-btn-sm px-btn-paper"
              onClick={() => onChangeStatus(report.id, 'OPEN')}>
              접수 상태로
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

const ReportAdminSection: React.FC = () => {
  const { reports, isLoading, errorMessage, statusFilter, setStatusFilter, comment, changeStatus } = useReportAdmin();

  return (
    <div className="px-card">
      <div className="px-head">
        <span>문의 관리</span>
        <span className="px-label text-[10px] num">{reports.length}건</span>
      </div>

      <div className="p-3 border-b-[3px] border-ink">
        <label className="px-label block mb-1.5" htmlFor="report-status-filter">
          처리 상태
        </label>
        <select
          id="report-status-filter"
          className="px-input"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}>
          {STATUS_FILTERS.map((filter) => (
            <option key={filter.value} value={filter.value}>
              {filter.label}
            </option>
          ))}
        </select>
      </div>

      {errorMessage && <p className="text-sm text-cherry px-3 pt-3">{errorMessage}</p>}

      {isLoading ? (
        <p className="px-title text-sm text-center py-16 animate-blink">문의 로드 중...</p>
      ) : reports.length === 0 ? (
        <div className="p-4">
          <div className="border-2 border-dashed border-ink/35 py-16 text-center">
            <p className="px-title text-sm">처리할 문의가 없습니다.</p>
          </div>
        </div>
      ) : (
        <div className="p-3 space-y-2.5">
          {reports.map((report) => (
            <ReportCard key={report.id} report={report} onComment={comment} onChangeStatus={changeStatus} />
          ))}
        </div>
      )}
    </div>
  );
};

export default ReportAdminSection;
