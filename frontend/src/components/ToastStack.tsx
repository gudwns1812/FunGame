import React, { useEffect, useSyncExternalStore } from 'react';
import { dismissToast, stackedToasts, subscribeToToasts, type Toast, type ToastTone } from '../utils/toast';

const VISIBLE_MS = 4000;

const TONE_LABEL: Record<ToastTone, string> = {
  error: '오류',
  success: '완료',
  info: '알림',
};

const TONE_CHIP: Record<ToastTone, string> = {
  error: 'px-chip px-chip-cherry',
  success: 'px-chip px-chip-grass',
  info: 'px-chip',
};

const TONE_ROLE: Record<ToastTone, 'alert' | 'status'> = {
  error: 'alert',
  success: 'status',
  info: 'status',
};

const ToastCard: React.FC<{ toast: Toast }> = ({ toast }) => {
  useEffect(() => {
    const hide = setTimeout(() => dismissToast(toast.id), VISIBLE_MS);
    return () => clearTimeout(hide);
  }, [toast.id]);

  return (
    <div role={TONE_ROLE[toast.tone]} className="px-card shadow-lg pointer-events-auto animate-toast-in">
      <div className="px-head flex items-center justify-between">
        <span className={TONE_CHIP[toast.tone]}>{TONE_LABEL[toast.tone]}</span>
        <button type="button" aria-label="알림 닫기" className="px-label" onClick={() => dismissToast(toast.id)}>
          ✕
        </button>
      </div>

      <p className="p-3 text-sm font-display">{toast.message}</p>
    </div>
  );
};

const ToastStack: React.FC = () => {
  const toasts = useSyncExternalStore(subscribeToToasts, stackedToasts);

  if (toasts.length === 0) return null;

  return (
    <div className="fixed bottom-0 left-1/2 -translate-x-1/2 z-[110] mb-2 w-[min(22rem,calc(100vw-1.5rem))] flex flex-col gap-2 pointer-events-none">
      {toasts.map((toast) => (
        <ToastCard key={toast.id} toast={toast} />
      ))}
    </div>
  );
};

export default ToastStack;
