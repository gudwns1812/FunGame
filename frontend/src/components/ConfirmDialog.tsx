import React, { useEffect, useSyncExternalStore } from 'react';
import { answerConfirm, confirmBeingAsked, subscribeToConfirms } from '../utils/confirm';

const MESSAGE_ID = 'confirm-dialog-message';

const ConfirmDialog: React.FC = () => {
  const asked = useSyncExternalStore(subscribeToConfirms, confirmBeingAsked);

  useEffect(() => {
    if (!asked) return;

    const cancelOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') answerConfirm(asked.id, false);
    };

    document.addEventListener('keydown', cancelOnEscape);
    return () => document.removeEventListener('keydown', cancelOnEscape);
  }, [asked]);

  if (!asked) return null;

  return (
    <div className="fixed inset-0 z-[105] flex items-center justify-center bg-ink/60 p-4 animate-fade-in">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={MESSAGE_ID}
        className="px-card w-full max-w-sm p-6 space-y-4 animate-scale-up">
        <p id={MESSAGE_ID} className="text-sm font-display">
          {asked.message}
        </p>

        <div className="flex items-center gap-2">
          <button
            autoFocus
            type="button"
            className="px-btn px-btn-primary flex-1 py-2.5"
            onClick={() => answerConfirm(asked.id, true)}>
            확인
          </button>
          <button
            type="button"
            className="px-btn px-btn-paper flex-1 py-2.5"
            onClick={() => answerConfirm(asked.id, false)}>
            취소
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmDialog;
