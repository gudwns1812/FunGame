import { renderHook } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useIdentifyLoggedInMember } from './useIdentifyLoggedInMember';
import type { MemberInfo } from '../types/auth';

const 반달: MemberInfo = { id: 1, loginId: 'login1', nickname: '반달', email: '반달@fun-game.club', role: 'USER' };
const 보름달: MemberInfo = { ...반달, nickname: '보름달' };

describe('로그인한 사람을 게임 로직에 알린다', () => {
  type Notify = (memberId: number, nickname: string) => void;
  let identify: Notify & { mock: { calls: unknown[][] } };
  let enterLobby: Notify & { mock: { calls: unknown[][] } };

  const renderWith = (user: MemberInfo | null, nickname: string) =>
    renderHook(
      ({ user, nickname }: { user: MemberInfo | null; nickname: string }) =>
        useIdentifyLoggedInMember({
          isAuthenticated: user !== null,
          user,
          nickname,
          identify,
          enterLobby,
        }),
      { initialProps: { user, nickname } },
    );

  beforeEach(() => {
    identify = vi.fn() as unknown as typeof identify;
    enterLobby = vi.fn() as unknown as typeof enterLobby;
  });

  it('로그인하면 회원 번호와 닉네임을 알린다.', () => {
    renderWith(반달, '');

    expect(identify).toHaveBeenCalledWith(1, '반달');
  });

  it('닉네임을 아직 모르면 로비에서 시작한다.', () => {
    renderWith(반달, '');

    expect(enterLobby).toHaveBeenCalledWith(1, '반달');
  });

  it('이미 닉네임을 알고 있으면 화면 상태를 건드리지 않는다.', () => {
    renderWith(반달, '반달');

    expect(enterLobby).not.toHaveBeenCalled();
  });

  it('닉네임을 바꿔도 로비로 보내지 않는다. 대기실이나 게임 중이면 튕겨 나간다.', () => {
    const { rerender } = renderWith(반달, '반달');

    rerender({ user: 보름달, nickname: '반달' });

    expect(identify).toHaveBeenLastCalledWith(1, '보름달');
    expect(enterLobby).not.toHaveBeenCalled();
  });

  it('로그인하지 않았으면 아무것도 알리지 않는다.', () => {
    renderWith(null, '');

    expect(identify).not.toHaveBeenCalled();
    expect(enterLobby).not.toHaveBeenCalled();
  });
});
