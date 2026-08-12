import React, { useEffect, useRef, useState } from 'react';
import type { RoomInvite } from '../types/presence';

interface InviteToastProps {
  invite: RoomInvite;
  onAccept: (invite: RoomInvite) => void;
  onDecline: (inviteId: string) => void;
  onExpire: (inviteId: string) => void;
}

const TICK_MS = 250;

const secondsUntilDeadline = (deadline: number) => Math.ceil((deadline - Date.now()) / 1000);

const InviteToast: React.FC<InviteToastProps> = ({ invite, onAccept, onDecline, onExpire }) => {
  const [remainingSeconds, setRemainingSeconds] = useState(invite.expiresInSeconds);
  const deadlineRef = useRef(Date.now() + invite.expiresInSeconds * 1000);

  useEffect(() => {
    deadlineRef.current = Date.now() + invite.expiresInSeconds * 1000;
    setRemainingSeconds(invite.expiresInSeconds);

    const refreshRemaining = () => setRemainingSeconds(secondsUntilDeadline(deadlineRef.current));
    const refreshOnTabReturn = () => {
      if (document.visibilityState === 'visible') refreshRemaining();
    };

    const countdown = setInterval(refreshRemaining, TICK_MS);
    document.addEventListener('visibilitychange', refreshOnTabReturn);

    return () => {
      clearInterval(countdown);
      document.removeEventListener('visibilitychange', refreshOnTabReturn);
    };
  }, [invite.inviteId, invite.expiresInSeconds]);

  useEffect(() => {
    if (remainingSeconds > 0) return;

    onExpire(invite.inviteId);
  }, [remainingSeconds, invite.inviteId, onExpire]);

  return (
    <div className="fixed top-0 left-1/2 -translate-x-1/2 z-[100] w-[min(22rem,calc(100vw-1.5rem))] animate-slide-down">
      <div className="px-card mt-2 shadow-lg">
        <div className="px-head flex items-center justify-between">
          <span className="px-label">방 초대</span>
          <span className="px-chip px-chip-gold">{Math.max(remainingSeconds, 0)}초</span>
        </div>

        <div className="p-3 flex flex-col gap-3">
          <p className="text-sm font-display">
            <strong>{invite.inviterNickname}</strong> 님이 초대했습니다
          </p>
          <p className="px-inset p-2 text-sm truncate">{invite.roomTitle}</p>

          <div className="flex gap-2">
            <button className="px-btn px-btn-sm px-btn-grass flex-1" onClick={() => onAccept(invite)}>
              수락
            </button>
            <button className="px-btn px-btn-sm px-btn-paper flex-1" onClick={() => onDecline(invite.inviteId)}>
              거절
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default InviteToast;
