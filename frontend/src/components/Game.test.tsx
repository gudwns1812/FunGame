/* @vitest-environment jsdom */

import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { describe, it, expect, vi, afterEach } from 'vitest';
import Game from './Game';

vi.mock('react-player', () => ({
  default: ({ src }: { src: string }) => <div data-testid="react-player" data-src={src} />,
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('Game UI 렌더링과 상호작용', () => {
  const mockProps = {
    players: [],
    roomId: '123',
    timeLeft: 30,
    totalTime: 30,
    currentVideoId: 'Test Question Content',
    onAnswerSubmit: vi.fn(),
    onSkipRound: vi.fn(),
    onFetchRank: vi.fn().mockResolvedValue(true),
    playerIndex: null,
    gameStartInfo: null,
    gameType: 'CS',
    roundEndInfo: null,
    roundIndex: 1,
    currentRound: 1,
    totalRound: 5,
    hint: '',
    logs: [],
  };

  it('CS 퀴즈 진입 시 문제 텍스트가 보여야 한다', () => {
    const { container } = render(<Game {...mockProps} />);

    const quizText = screen.getByText('Test Question Content');
    expect(quizText).toBeTruthy();

    const panel = container.querySelector('.h-\\[550px\\]');
    expect(panel).not.toBeNull();
  });

  it('CS 퀴즈에서 고정 background YouTube audio를 재생해야 한다', () => {
    render(<Game {...mockProps} />);

    const player = screen.getByTestId('react-player');
    expect(player.getAttribute('data-src')).toBe('https://www.youtube.com/watch?v=U34kLXjdw90');
  });

  it('CS 라운드 종료 시 answer는 그대로 보여주고 explanation을 별도로 보여준다', () => {
    render(
      <Game
        {...mockProps}
        roundEndInfo={{
          answer: 'TCP',
          explanation: '전송 계층에서 신뢰성 있는 데이터 전달을 담당합니다.',
          winner: null,
        }}
      />,
    );

    expect(screen.getByText('정답')).toBeTruthy();
    expect(screen.getByText('TCP')).toBeTruthy();
    expect(screen.getByText('해설')).toBeTruthy();
    expect(screen.getByText('전송 계층에서 신뢰성 있는 데이터 전달을 담당합니다.')).toBeTruthy();
  });

  it('노래 퀴즈 라운드 종료 시 전체 answer 문자열을 그대로 보여준다', () => {
    render(
      <Game
        {...mockProps}
        gameType="SONG"
        roundEndInfo={{
          answer: '아이유 - 좋은날',
          explanation: null,
          winner: null,
        }}
      />,
    );

    expect(screen.getByText('아이유 - 좋은날')).toBeTruthy();
  });

  it('첫 문제가 시작되기 전에는 스킵 버튼을 누를 수 없다', () => {
    render(<Game {...mockProps} currentRound={0} />);

    const skipButton = screen.getByRole('button', { name: '스킵' }) as HTMLButtonElement;
    expect(skipButton.disabled).toBe(true);

    fireEvent.click(skipButton);
    expect(mockProps.onSkipRound).not.toHaveBeenCalled();
  });

  it('라운드가 시작되면 스킵 버튼으로 투표할 수 있다', () => {
    render(<Game {...mockProps} currentRound={1} />);

    const skipButton = screen.getByRole('button', { name: '스킵' }) as HTMLButtonElement;
    expect(skipButton.disabled).toBe(false);

    fireEvent.click(skipButton);
    expect(mockProps.onSkipRound).toHaveBeenCalled();
  });

  it('전역에서 Enter 입력 시 채팅 입력창으로 포커스가 이동해야 한다', () => {
    render(<Game {...mockProps} />);

    const chatInput = screen.getByRole('textbox');

    chatInput.blur();
    expect(document.activeElement).not.toBe(chatInput);

    fireEvent.keyDown(window, { key: 'Enter', code: 'Enter' });

    expect(document.activeElement).toBe(chatInput);
  });
});
