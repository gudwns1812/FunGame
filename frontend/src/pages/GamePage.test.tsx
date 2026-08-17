import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import GamePage from './GamePage';

vi.mock('axios');
vi.mock('react-player', () => ({
  default: ({ src }: { src: string }) => <div data-testid="react-player" data-src={src} />,
}));

const mockedAxios = axios as unknown as { post: ReturnType<typeof vi.fn> };

const renderGamePage = (roomId: string) =>
  render(
    <GamePage
      players={[]}
      roomId={roomId}
      timeLeft={30}
      totalTime={30}
      currentVideoId="문제"
      logs={[]}
      onAnswerSubmit={vi.fn()}
      onSkipRound={vi.fn()}
      onFetchRank={vi.fn().mockResolvedValue(undefined)}
      gameStartInfo={null}
      gameType="SONG"
      roundEndInfo={null}
      currentRound={1}
      totalRound={5}
      hint=""
    />,
  );

describe('GamePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: null } });
  });

  it('진행 중 화면 상단에 신고 버튼이 있다', () => {
    renderGamePage('42');

    expect(screen.getByRole('button', { name: '신고' })).toBeInTheDocument();
  });

  it('방 번호가 없으면 신고 버튼을 내보내지 않는다', () => {
    renderGamePage('');

    expect(screen.queryByRole('button', { name: '신고' })).not.toBeInTheDocument();
  });
});
