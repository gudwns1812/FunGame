import React, { useEffect } from 'react';

interface KickedNoticeProps {
  message: string;
  onClose: () => void;
}

const VISIBLE_MS = 5000;

const KickedNotice: React.FC<KickedNoticeProps> = ({ message, onClose }) => {
  useEffect(() => {
    const hide = setTimeout(onClose, VISIBLE_MS);
    return () => clearTimeout(hide);
  }, [onClose]);

  return (
    <div
      role="status"
      className="fixed top-0 left-1/2 -translate-x-1/2 z-[100] w-[min(22rem,calc(100vw-1.5rem))] animate-slide-down">
      <div className="px-card mt-2 shadow-lg">
        <div className="px-head flex items-center justify-between">
          <span className="px-label">알림</span>
          <button type="button" aria-label="알림 닫기" className="px-label" onClick={onClose}>
            ✕
          </button>
        </div>

        <p className="p-3 text-sm font-display">{message}</p>
      </div>
    </div>
  );
};

export default KickedNotice;
