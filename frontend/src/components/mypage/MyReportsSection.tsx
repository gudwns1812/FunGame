import React from 'react';
import { useMyReports } from '../../hooks/useMyReports';
import { gameTypeLabelOf, reasonLabelOf, sourceLabelOf, statusLabelOf } from '../../utils/reportReasons';
import type { MyReport } from '../../types/report';

const ReportCard: React.FC<{ report: MyReport }> = ({ report }) => (
  <div className="px-card-sm p-3 space-y-2.5">
    <div className="flex flex-wrap items-center justify-between gap-2">
      <div className="flex items-center gap-2 min-w-0">
        <span className="px-chip num shrink-0">#{report.id}</span>
        <span className="px-title text-sm truncate">{reasonLabelOf(report.reason)}</span>
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

    {report.detail && <p className="px-inset px-3 py-2 text-xs leading-relaxed">{report.detail}</p>}

    {report.comments.length === 0 ? (
      <p className="px-label text-[10px]">아직 확인 중입니다. 처리되면 여기에 답변이 올라옵니다.</p>
    ) : (
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
  </div>
);

const MyReportsSection: React.FC = () => {
  const { reports, isLoading, errorMessage } = useMyReports();

  return (
    <div className="px-card">
      <div className="px-head">
        <span>내 문의</span>
        <span className="px-label text-[10px] num">{reports.length}건</span>
      </div>

      {isLoading ? (
        <p className="px-title text-sm text-center py-16 animate-blink">문의 내역 로드 중...</p>
      ) : errorMessage ? (
        <p className="text-sm text-cherry text-center py-16">{errorMessage}</p>
      ) : reports.length === 0 ? (
        <div className="p-4">
          <div className="border-2 border-dashed border-ink/35 py-16 text-center space-y-2">
            <p className="px-title text-sm">접수한 문의가 없습니다.</p>
            <p className="px-label text-[10px]">게임 중 상단의 신고 버튼이나 문의·신고 페이지로 접수할 수 있습니다.</p>
          </div>
        </div>
      ) : (
        <div className="p-3 space-y-2.5">
          {reports.map((report) => (
            <ReportCard key={report.id} report={report} />
          ))}
        </div>
      )}
    </div>
  );
};

export default MyReportsSection;
