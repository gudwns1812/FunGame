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
    { id: '1', name: 'Alice', score: 100, isHost: false, isReady: true, colorIndex: 0 },
    { id: '2', name: 'Bob', score: 200, isHost: false, isReady: true, colorIndex: 1 },
    { id: '3', name: 'Charlie', score: 150, isHost: false, isReady: true, colorIndex: 2 },
  ];

  it('플레이어를 점수 내림차순으로 정렬하여 렌더링한다', () => {
    render(<RankingList players={mockPlayers} roundEndInfo={null} />);
    
    const items = screen.getAllByText(/#/);
    expect(items[0]).toHaveTextContent('#1'); // Bob (200)
    expect(items[1]).toHaveTextContent('#2'); // Charlie (150)
    expect(items[2]).toHaveTextContent('#3'); // Alice (100)
    
    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('Charlie')).toBeInTheDocument();
    expect(screen.getByText('Alice')).toBeInTheDocument();
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
