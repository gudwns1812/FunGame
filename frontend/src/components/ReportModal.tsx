import React, { useState } from 'react';
import type { ReportReason } from '../types/report';
import { reasonChoicesFor } from '../utils/reportReasons';

interface ReportModalProps {
  gameType: string | null;
  onSubmit: (reason: ReportReason, detail: string | null) => void;
  onClose: () => void;
  isSubmitting: boolean;
  errorMessage: string | null;
  isSubmitted: boolean;
}

const ReportModal: React.FC<ReportModalProps> = ({
  gameType,
  onSubmit,
  onClose,
  isSubmitting,
  errorMessage,
  isSubmitted,
}) => {
  const [isWritingDetail, setIsWritingDetail] = useState(false);
  const [detail, setDetail] = useState('');

  const choose = (reason: ReportReason) => {
    if (reason === 'ETC') {
      setIsWritingDetail(true);
      return;
    }

    onSubmit(reason, null);
  };

  const submitWrittenDetail = () => {
    if (!detail.trim()) {
      return;
    }

    onSubmit('ETC', detail.trim());
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/60 p-4 animate-fade-in">
      <div className="px-card w-full max-w-xs p-6 space-y-4 animate-scale-up">
        <h2 className="px-title text-lg border-b-[3px] border-ink pb-2">문제 신고</h2>

        {isSubmitted ? (
          <p className="text-sm">신고를 접수했습니다. 확인 뒤 고치겠습니다.</p>
        ) : (
          <>
            <p className="px-label text-[10px]">어느 문제였는지는 서버가 알고 있으니 사유만 골라주세요.</p>

            <div className="space-y-2">
              {reasonChoicesFor(gameType).map((choice) => (
                <button
                  key={choice.reason}
                  type="button"
                  className="px-btn px-btn-paper w-full py-2.5"
                  disabled={isSubmitting}
                  onClick={() => choose(choice.reason)}>
                  {choice.label}
                </button>
              ))}
            </div>

            {isWritingDetail && (
              <div className="space-y-2">
                <label className="px-label block" htmlFor="report-detail">
                  무엇이 잘못됐나요?
                </label>
                <textarea
                  id="report-detail"
                  className="px-input w-full h-20"
                  value={detail}
                  onChange={(event) => setDetail(event.target.value)}
                />
                <button
                  type="button"
                  className="px-btn px-btn-primary w-full py-2.5"
                  disabled={isSubmitting}
                  onClick={submitWrittenDetail}>
                  접수
                </button>
              </div>
            )}
          </>
        )}

        {errorMessage && <p className="text-sm text-cherry">{errorMessage}</p>}

        <button type="button" className="px-btn px-btn-paper w-full py-2.5" onClick={onClose}>
          닫기
        </button>
      </div>
    </div>
  );
};

export default ReportModal;
