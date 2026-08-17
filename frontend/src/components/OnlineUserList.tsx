import React, { useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { useOnlineMembers } from '../hooks/useOnlineMembers';
import { notifyError } from '../utils/toast';
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

const GROUPS = [
  { key: 'LOBBY', label: '로비에 있음' },
  { key: 'ELSEWHERE', label: '다른 방에 있음' },
] as const;

type GroupKey = (typeof GROUPS)[number]['key'];

const groupOf = (member: OnlineMember): GroupKey => (member.status === 'LOBBY' ? 'LOBBY' : 'ELSEWHERE');

const groupsCollapsedWhileInviting = (invitingRoomId?: string | null): GroupKey[] =>
  invitingRoomId ? ['ELSEWHERE'] : [];

const MagnifierIcon: React.FC = () => (
  <svg viewBox="0 0 12 12" className="w-3 h-3" fill="currentColor" shapeRendering="crispEdges" aria-hidden="true">
    <rect x="2" y="1" width="5" height="1" />
    <rect x="2" y="7" width="5" height="1" />
    <rect x="1" y="2" width="1" height="5" />
    <rect x="7" y="2" width="1" height="5" />
    <rect x="8" y="8" width="2" height="2" />
    <rect x="10" y="10" width="2" height="2" />
  </svg>
);

const OnlineUserList: React.FC<OnlineUserListProps> = ({ invitingRoomId }) => {
  const members = useOnlineMembers(true);
  const [invitedMemberIds, setInvitedMemberIds] = useState<number[]>([]);
  const [collapsedGroups, setCollapsedGroups] = useState<GroupKey[]>(() =>
    groupsCollapsedWhileInviting(invitingRoomId),
  );
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [searchInput, setSearchInput] = useState('');
  const expiryTimers = useRef<Map<number, ReturnType<typeof setTimeout>>>(new Map());

  useEffect(() => {
    const timers = expiryTimers.current;
    return () => timers.forEach(clearTimeout);
  }, []);

  const keyword = searchInput.trim().toLowerCase();
  const isFiltering = keyword.length > 0;
  const matchedMembers = isFiltering
    ? members.filter((member) => member.nickname.toLowerCase().includes(keyword))
    : members;

  const nothingToShowMessage =
    members.length === 0
      ? '아무도 접속해 있지 않습니다'
      : matchedMembers.length === 0
        ? '검색 결과가 없습니다'
        : null;

  const closeSearch = () => {
    setIsSearchOpen(false);
    setSearchInput('');
  };

  const toggleSearch = () => (isSearchOpen ? closeSearch() : setIsSearchOpen(true));

  const toggleGroup = (key: GroupKey) => {
    setCollapsedGroups((collapsed) =>
      collapsed.includes(key) ? collapsed.filter((group) => group !== key) : [...collapsed, key],
    );
  };

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
      notifyError(error?.response?.data?.error?.message || '초대를 보내지 못했습니다.');
    }
  };

  const renderMember = (member: OnlineMember) => {
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
  };

  return (
    <aside className="px-card w-full md:w-56 shrink-0 flex flex-col min-h-0">
      <div className="px-head flex items-center justify-between">
        {isSearchOpen ? (
          <input
            autoFocus
            type="text"
            className="px-input-line"
            placeholder="닉네임 검색"
            aria-label="닉네임으로 접속자 검색"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Escape') closeSearch();
            }}
          />
        ) : (
          <span className="px-label">접속 중</span>
        )}

        <span className="flex items-center gap-1.5 shrink-0">
          <button
            type="button"
            aria-label="닉네임 검색"
            aria-expanded={isSearchOpen}
            className={isSearchOpen ? 'px-chip px-chip-cherry' : 'px-chip'}
            onClick={toggleSearch}>
            <MagnifierIcon />
          </button>
          <span className="px-chip">{members.length}</span>
        </span>
      </div>

      <div className="flex-1 min-h-0 scroll-y custom-scrollbar p-2 flex flex-col gap-2">
        {nothingToShowMessage ? (
          <p className="px-label text-center py-4 opacity-60">{nothingToShowMessage}</p>
        ) : (
          GROUPS.map(({ key, label }) => {
            const groupMembers = matchedMembers.filter((member) => groupOf(member) === key);
            const isCollapsed = !isFiltering && collapsedGroups.includes(key);

            return (
              <section key={key} className="flex flex-col gap-2">
                <button
                  type="button"
                  className="px-inset px-tap w-full p-2 flex items-center justify-between gap-2"
                  aria-expanded={!isCollapsed}
                  onClick={() => toggleGroup(key)}>
                  <span className="flex items-center gap-1.5 min-w-0">
                    <span className="px-label opacity-60">{isCollapsed ? '▶' : '▼'}</span>
                    <span className="px-label truncate">{label}</span>
                  </span>
                  <span className="px-chip num">{groupMembers.length}</span>
                </button>

                {!isCollapsed &&
                  (groupMembers.length === 0 ? (
                    <p className="px-label text-center py-2 opacity-60">아무도 없습니다</p>
                  ) : (
                    groupMembers.map(renderMember)
                  ))}
              </section>
            );
          })
        )}
      </div>
    </aside>
  );
};

export default OnlineUserList;
