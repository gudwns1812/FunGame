/**
 * @vitest-environment jsdom
 */
import { render, screen, cleanup } from '@testing-library/react';
import { describe, it, expect, afterEach } from 'vitest';
import '@testing-library/jest-dom/vitest';
import RankingItem from './RankingItem';
import { Player } from '../types/game';

describe('RankingItem', () => {
  afterEach(() => {
    cleanup();
  });

  const mockPlayer: Player = {
    id: 'user1',
    name: 'Player 1',
    score: 100,
    isHost: false,
    isReady: true,
    colorIndex: 0,
  };

  it('플레이어 이름과 점수를 올바르게 렌더링한다', () => {
    render(
      <RankingItem
        player={mockPlayer}
        rank={1}
        isFirst={false}
        isWinner={false}
        color="#25c0f4"
      />
    );

    expect(screen.getByText('Player 1')).toBeInTheDocument();
    expect(screen.getByText('100')).toBeInTheDocument();
    expect(screen.getByText('#1')).toBeInTheDocument();
  });

  it('isWinner가 true일 때 animate-shimmer 클래스를 포함한다', () => {
    const { container } = render(
      <RankingItem
        player={mockPlayer}
        rank={1}
        isFirst={false}
        isWinner={true}
        color="#25c0f4"
      />
    );

    const rankingItem = container.firstChild;
    expect(rankingItem).toHaveClass('animate-shimmer');
  });

  it('isFirst가 true일 때 특수한 스타일이 적용된다', () => {
    render(
      <RankingItem
        player={mockPlayer}
        rank={1}
        isFirst={true}
        isWinner={false}
        color="#25c0f4"
      />
    );

    const nameElement = screen.getByText('Player 1');
    expect(nameElement).toHaveStyle({ color: '#ffffff' });
  });
});
