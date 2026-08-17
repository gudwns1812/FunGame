import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import HangmanPage from './HangmanPage';
import type { HangmanStatus } from '../types/game';

vi.mock('axios');

const mockedAxios = axios as unknown as { post: ReturnType<typeof vi.fn> };

const STATUS: HangmanStatus = {
  currentDisplay: '_ _ _',
  wrongLetters: [],
  remainingTries: 6,
  currentTurnPlayer: '나',
  currentTurnMemberId: 1,
  isGameOver: false,
  isWin: false,
};

const renderHangmanPage = (roomId: string, onLeave = vi.fn()) =>
  render(
    <MemoryRouter>
      <HangmanPage
        status={STATUS}
        onGuess={vi.fn()}
        myMemberId={1}
        logs={[]}
        players={[]}
        onSendMessage={vi.fn()}
        roomId={roomId}
        onLeave={onLeave}
      />
    </MemoryRouter>,
  );

describe('HangmanPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: null } });
  });

  it('행맨 진행 중 화면 상단에 신고 버튼이 있다', () => {
    renderHangmanPage('42');

    expect(screen.getByRole('button', { name: '신고' })).toBeInTheDocument();
  });

  it('방 번호가 없으면 신고 버튼을 내보내지 않는다', () => {
    renderHangmanPage('');

    expect(screen.queryByRole('button', { name: '신고' })).not.toBeInTheDocument();
  });

  it('나가기를 눌러도 확인하기 전에는 방을 떠나지 않는다', async () => {
    const onLeave = vi.fn();
    renderHangmanPage('42', onLeave);

    await userEvent.click(screen.getByRole('button', { name: '나가기' }));

    expect(screen.getByRole('dialog', { name: '게임 나가기' })).toBeInTheDocument();
    expect(onLeave).not.toHaveBeenCalled();
  });

  it('확인 모달에서 나가기를 누르면 방을 떠난다', async () => {
    const onLeave = vi.fn();
    renderHangmanPage('42', onLeave);

    await userEvent.click(screen.getByRole('button', { name: '나가기' }));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: '나가기' }));

    expect(onLeave).toHaveBeenCalledTimes(1);
  });

  it('확인 모달에서 취소하면 게임 화면이 그대로 남는다', async () => {
    const onLeave = vi.fn();
    renderHangmanPage('42', onLeave);

    await userEvent.click(screen.getByRole('button', { name: '나가기' }));
    await userEvent.click(screen.getByRole('button', { name: '취소' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(onLeave).not.toHaveBeenCalled();
  });
});
