/**
 * @vitest-environment jsdom
 */
import { render, screen, cleanup } from '@testing-library/react';
import { describe, it, expect, afterEach } from 'vitest';
import '@testing-library/jest-dom/vitest';
import RankingItem from './RankingItem';
import type { Player } from '../types/game';

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
        rank={4}
        isWinner={false}
        color="#25c0f4"
      />
    );

    expect(screen.getByText('Player 1')).toBeInTheDocument();
    expect(screen.getByText('100')).toBeInTheDocument();
    expect(screen.getByText('#4')).toBeInTheDocument();
  });

  it('isWinner가 true일 때 animate-shimmer 클래스를 포함한다', () => {
    const { container } = render(
      <RankingItem
        player={mockPlayer}
        rank={1}
        isWinner={true}
        color="#25c0f4"
      />
    );

    const rankingItem = container.firstChild;
    expect(rankingItem).toHaveClass('animate-shimmer');
  });

  it('1위일 때 1등 뱃지 이미지를 표시한다', () => {
    render(
      <RankingItem
        player={mockPlayer}
        rank={1}
        isWinner={false}
        color="#25c0f4"
      />
    );

    const badge = screen.getByAltText('1st Badge');
    expect(badge).toBeInTheDocument();
  });

  it('2위일 때 2등 뱃지 이미지를 표시한다', () => {
    render(
      <RankingItem
        player={mockPlayer}
        rank={2}
        isWinner={false}
        color="#25c0f4"
      />
    );

    const badge = screen.getByAltText('2nd Badge');
    expect(badge).toBeInTheDocument();
  });

  it('3위일 때 3등 뱃지 이미지를 표시한다', () => {
    render(
      <RankingItem
        player={mockPlayer}
        rank={3}
        isWinner={false}
        color="#25c0f4"
      />
    );

    const badge = screen.getByAltText('3rd Badge');
    expect(badge).toBeInTheDocument();
  });

  it('4위 이상일 때 뱃지 이미지를 표시하지 않는다', () => {
    render(
      <RankingItem
        player={mockPlayer}
        rank={4}
        isWinner={false}
        color="#25c0f4"
      />
    );

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });
});
