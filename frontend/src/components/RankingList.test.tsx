/**
 * @vitest-environment jsdom
 */
import { render, screen, cleanup } from '@testing-library/react';
import { describe, it, expect, afterEach } from 'vitest';
import '@testing-library/jest-dom/vitest';
import RankingList from './RankingList';
import type { Player, RoundEndInfo } from '../types/game';

describe('RankingList', () => {
  afterEach(() => {
    cleanup();
  });

  const mockPlayers: Player[] = [
    { memberId: 1, name: 'Alice', score: 100, rank: 3, isHost: false, isReady: true, colorIndex: 0 },
    { memberId: 2, name: 'Bob', score: 200, rank: 1, isHost: false, isReady: true, colorIndex: 1 },
    { memberId: 3, name: 'Charlie', score: 150, rank: 2, isHost: false, isReady: true, colorIndex: 2 },
  ];

  const player = (memberId: number, name: string, score: number, rank?: number): Player => ({
    memberId,
    name,
    score,
    rank,
    isHost: false,
    isReady: true,
  });

  it('서버가 매긴 순위대로 정렬하여 렌더링한다', () => {
    const { container } = render(<RankingList players={mockPlayers} roundEndInfo={null} />);

    expect(container.textContent).toMatch(/Bob[\s\S]*Charlie[\s\S]*Alice/);

    expect(screen.getByAltText('1st Badge')).toBeInTheDocument();
    expect(screen.getByAltText('2nd Badge')).toBeInTheDocument();
    expect(screen.getByAltText('3rd Badge')).toBeInTheDocument();
  });

  it('같은 순위 안에서는 회원 번호 순이고 같은 배지를 받는다', () => {
    const { container } = render(
      <RankingList
        players={[player(3, 'Charlie', 1, 3), player(2, 'Bob', 4, 1), player(1, 'Alice', 4, 1)]}
        roundEndInfo={null}
      />,
    );

    expect(container.textContent).toMatch(/Alice[\s\S]*Bob[\s\S]*Charlie/);
    expect(screen.getAllByAltText('1st Badge')).toHaveLength(2);
    expect(screen.getByAltText('3rd Badge')).toBeInTheDocument();
  });

  it('아직 점수가 없는 사람에게는 1위여도 배지를 붙이지 않는다', () => {
    render(<RankingList players={[player(1, 'Alice', 0, 1), player(2, 'Bob', 0, 1)]} roundEndInfo={null} />);

    expect(screen.queryByAltText('1st Badge')).not.toBeInTheDocument();
  });

  it('순위를 아직 받지 못한 사람은 맨 뒤에 둔다', () => {
    const { container } = render(
      <RankingList players={[player(4, 'Dave', 0), player(2, 'Bob', 2, 1)]} roundEndInfo={null} />,
    );

    expect(container.textContent).toMatch(/Bob[\s\S]*Dave/);
  });

  it('roundEndInfo.winner에 해당하는 플레이어를 승자로 식별한다', () => {
    const roundEndInfo: RoundEndInfo = {
      answer: 'test',
      winner: 'Alice',
      explanation: null,
    };

    render(<RankingList players={mockPlayers} roundEndInfo={roundEndInfo} />);
    
    // Alice 항목이 animate-shimmer 클래스를 가지고 있는지 확인
    // RankingItem 컴포넌트가 최상단 div에 animate-shimmer를 추가하므로 
    // Alice 텍스트를 포함하는 조상 div를 찾아 확인합니다.
    const aliceText = screen.getByText('Alice');
    const aliceItem = aliceText.closest('.animate-shimmer');
    expect(aliceItem).toBeInTheDocument();
  });

  it('다중 우승자(쉼표 구분)를 지원한다', () => {
    const roundEndInfo: RoundEndInfo = {
      answer: 'test',
      winner: 'Alice, Bob',
      explanation: null,
    };

    render(<RankingList players={mockPlayers} roundEndInfo={roundEndInfo} />);
    
    expect(screen.getByText('Alice').closest('.animate-shimmer')).toBeInTheDocument();
    expect(screen.getByText('Bob').closest('.animate-shimmer')).toBeInTheDocument();
    expect(screen.getByText('Charlie').closest('.animate-shimmer')).not.toBeInTheDocument();
  });

  it('winner가 "없음"인 경우 아무도 승자로 표시하지 않는다', () => {
    const roundEndInfo: RoundEndInfo = {
      answer: 'test',
      winner: '없음',
      explanation: null,
    };

    const { container } = render(<RankingList players={mockPlayers} roundEndInfo={roundEndInfo} />);
    expect(container.querySelector('.animate-shimmer')).not.toBeInTheDocument();
  });
});
