import { render, screen, act, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import InviteToast from './InviteToast';
import type { RoomInvite } from '../types/presence';

const invite: RoomInvite = {
  inviteId: 'invite-1',
  roomId: 7,
  roomTitle: 'K-POP 퀴즈방',
  gameType: 'SONG',
  inviterNickname: '짱구',
  expiresInSeconds: 30,
};

const renderToast = (overrides: Partial<Parameters<typeof InviteToast>[0]> = {}) => {
  const props = {
    invite,
    onAccept: vi.fn(),
    onDecline: vi.fn(),
    onExpire: vi.fn(),
    ...overrides,
  };
  render(<InviteToast {...props} />);
  return props;
};

describe('InviteToast', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('누가 어느 방으로 초대했는지 보여준다', () => {
    renderToast();

    expect(screen.getByText('짱구')).toBeInTheDocument();
    expect(screen.getByText('K-POP 퀴즈방')).toBeInTheDocument();
  });

  it('남은 시간을 초 단위로 센다', () => {
    renderToast();
    expect(screen.getByText('30초')).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(3000);
    });

    expect(screen.getByText('27초')).toBeInTheDocument();
  });

  it('시간이 다 되면 만료를 알린다', () => {
    const { onExpire } = renderToast();

    act(() => {
      vi.advanceTimersByTime(30000);
    });

    expect(onExpire).toHaveBeenCalledWith('invite-1');
  });

  it('시간이 남아 있으면 만료를 알리지 않는다', () => {
    const { onExpire } = renderToast();

    act(() => {
      vi.advanceTimersByTime(29000);
    });

    expect(onExpire).not.toHaveBeenCalled();
  });

  it('남은 시간은 0초 아래로 내려가지 않는다', () => {
    renderToast();

    act(() => {
      vi.advanceTimersByTime(35000);
    });

    expect(screen.getByText('0초')).toBeInTheDocument();
  });

  it('백그라운드에서 타이머가 멈춰도 탭으로 돌아오면 남은 시간을 바로잡는다', () => {
    const { onExpire } = renderToast();
    vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('visible');

    act(() => {
      vi.setSystemTime(Date.now() + 31000);
      document.dispatchEvent(new Event('visibilitychange'));
    });

    expect(screen.getByText('0초')).toBeInTheDocument();
    expect(onExpire).toHaveBeenCalledWith('invite-1');
  });

  it('수락을 누르면 초대를 그대로 넘긴다', () => {
    const { onAccept } = renderToast();

    fireEvent.click(screen.getByRole('button', { name: '수락' }));

    expect(onAccept).toHaveBeenCalledWith(invite);
  });

  it('거절을 누르면 초대 번호를 넘긴다', () => {
    const { onDecline } = renderToast();

    fireEvent.click(screen.getByRole('button', { name: '거절' }));

    expect(onDecline).toHaveBeenCalledWith('invite-1');
  });
});
