import React from 'react';

const TITLE_ID = 'leave-game-confirm-title';

interface LeaveGameConfirmProps {
  onConfirm: () => void;
  onCancel: () => void;
}

const LeaveGameConfirm: React.FC<LeaveGameConfirmProps> = ({ onConfirm, onCancel }) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/60 p-4 animate-fade-in">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={TITLE_ID}
        className="px-card w-full max-w-xs p-6 space-y-4 animate-scale-up">
        <h2 id={TITLE_ID} className="px-title text-lg border-b-[3px] border-ink pb-2">
          게임 나가기
        </h2>

        <p className="text-sm">진행 중인 게임에서 나가고 방 목록으로 돌아갈까요?</p>
        <p className="px-label text-[10px]">다시 들어오면 점수와 현재 라운드를 이어받습니다.</p>

        <div className="flex items-center gap-2 pt-1">
          <button type="button" className="px-btn px-btn-primary flex-1 py-2.5" onClick={onConfirm}>
            나가기
          </button>
          <button type="button" className="px-btn px-btn-paper flex-1 py-2.5" onClick={onCancel}>
            취소
          </button>
        </div>
      </div>
    </div>
  );
};

export default LeaveGameConfirm;
