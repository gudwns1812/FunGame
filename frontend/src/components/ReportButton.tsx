import React, { useState } from 'react';
import ReportModal from './ReportModal';
import { useReport } from '../hooks/useReport';
import type { ReportReason } from '../types/report';

interface ReportButtonProps {
  roomId: number | null;
  gameType: string | null;
}

const ReportButton: React.FC<ReportButtonProps> = ({ roomId, gameType }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const { submitReport, isSubmitting } = useReport();

  if (roomId === null) {
    return null;
  }

  const open = () => {
    setIsSubmitted(false);
    setErrorMessage(null);
    setIsOpen(true);
  };

  const submit = async (reason: ReportReason, detail: string | null) => {
    const result = await submitReport({ source: 'IN_GAME', roomId, reason, detail, gameType: null });

    setIsSubmitted(result.ok);
    setErrorMessage(result.message);
  };

  return (
    <>
      <button type="button" className="px-btn px-btn-sm px-btn-paper" onClick={open}>
        신고
      </button>

      {isOpen && (
        <ReportModal
          gameType={gameType}
          onSubmit={submit}
          onClose={() => setIsOpen(false)}
          isSubmitting={isSubmitting}
          errorMessage={errorMessage}
          isSubmitted={isSubmitted}
        />
      )}
    </>
  );
};

export default ReportButton;
