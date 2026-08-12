import React, { useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { useOnlineMembers } from '../hooks/useOnlineMembers';
import type { OnlineMember, PresenceStatus } from '../types/presence';

interface OnlineUserListProps {
  /** 대기실에서만 초대를 보낼 수 있다. 로비에서는 목록만 보여준다. */
  invitingRoomId?: string | null;
}

const STATUS_LABEL: Record<PresenceStatus, string> = {
  LOBBY: '로비',
  WAITING: '대기실',
  PLAYING: '게임중',
};

const STATUS_CHIP: Record<PresenceStatus, string> = {
  LOBBY: 'px-chip px-chip-grass',
  WAITING: 'px-chip px-chip-sea',
  PLAYING: 'px-chip px-chip-cherry',
};

const FALLBACK_INVITE_LIFETIME_SECONDS = 30;

const OnlineUserList: React.FC<OnlineUserListProps> = ({ invitingRoomId }) => {
  const members = useOnlineMembers(true);
  const [invitedMemberIds, setInvitedMemberIds] = useState<number[]>([]);
  const expiryTimers = useRef<Map<number, ReturnType<typeof setTimeout>>>(new Map());

  useEffect(() => {
    const timers = expiryTimers.current;
    return () => timers.forEach(clearTimeout);
  }, []);

  const forgetInvite = (memberId: number) => {
    setInvitedMemberIds((invited) => invited.filter((id) => id !== memberId));
    expiryTimers.current.delete(memberId);
  };

  const forgetWhenInviteExpires = (memberId: number, expiresInSeconds: number) => {
    clearTimeout(expiryTimers.current.get(memberId));
    expiryTimers.current.set(
      memberId,
      setTimeout(() => forgetInvite(memberId), expiresInSeconds * 1000),
    );
  };

  const sendInvite = async (member: OnlineMember) => {
    if (!invitingRoomId) return;

    setInvitedMemberIds((invited) => [...invited, member.memberId]);
    try {
      const response = await axios.post(`/api/rooms/${invitingRoomId}/invites`, {
        targetMemberId: member.memberId,
      });
      const expiresInSeconds = response.data?.data?.expiresInSeconds ?? FALLBACK_INVITE_LIFETIME_SECONDS;
      forgetWhenInviteExpires(member.memberId, expiresInSeconds);
    } catch (error: any) {
      forgetInvite(member.memberId);
      window.alert(error?.response?.data?.error?.message || '초대를 보내지 못했습니다.');
    }
  };

  return (
    <aside className="px-card w-full md:w-56 shrink-0 flex flex-col min-h-0">
      <div className="px-head flex items-center justify-between">
        <span className="px-label">접속 중</span>
        <span className="px-chip">{members.length}</span>
      </div>

      <div className="flex-1 min-h-0 scroll-y custom-scrollbar p-2 flex flex-col gap-2">
        {members.length === 0 && (
          <p className="px-label text-center py-4 opacity-60">아무도 접속해 있지 않습니다</p>
        )}

        {members.map((member) => {
          const isInvitable = Boolean(invitingRoomId) && member.status === 'LOBBY';
          const isAlreadyInvited = invitedMemberIds.includes(member.memberId);

          return (
            <div key={member.memberId} className="px-inset p-2 flex items-center justify-between gap-2">
              <div className="min-w-0">
                <p className="text-sm font-display truncate">{member.nickname}</p>
                <span className={STATUS_CHIP[member.status]}>{STATUS_LABEL[member.status]}</span>
              </div>

              {invitingRoomId && (
                <button
                  className="px-btn px-btn-sm px-btn-sea"
                  disabled={!isInvitable || isAlreadyInvited}
                  onClick={() => sendInvite(member)}>
                  {isAlreadyInvited ? '보냄' : '초대'}
                </button>
              )}
            </div>
          );
        })}
      </div>
    </aside>
  );
};

export default OnlineUserList;
