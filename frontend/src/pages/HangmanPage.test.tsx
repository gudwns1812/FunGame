import { render, screen } from '@testing-library/react';
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

const renderHangmanPage = (roomId: string) =>
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
});
