import { render, screen, within } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import Result from './Result';
import type { Player } from '../types/game';

const ranked = (memberId: number, name: string, score: number, rank: number, winner = false): Player => ({
  memberId,
  name,
  score,
  rank,
  winner,
  isHost: false,
  isReady: false,
});

const renderResult = (rankings: Player[]) =>
  render(<Result rankings={rankings} onBackToLobby={() => {}} onBackToRoom={() => {}} />);

const rankRows = () => screen.getAllByTestId('result-row');

describe('결과 화면', () => {
  it('서버가 매긴 순위대로 배지를 붙여 동점자는 같은 배지를 받는다', () => {
    renderResult([
      ranked(2, '나중가입', 5, 1, true),
      ranked(3, '동점자', 5, 1, true),
      ranked(1, '먼저가입', 3, 3),
    ]);

    const rows = rankRows();
    expect(within(rows[0]).getByAltText('1위')).toBeInTheDocument();
    expect(within(rows[1]).getByAltText('1위')).toBeInTheDocument();
    expect(within(rows[2]).getByAltText('3위')).toBeInTheDocument();
  });

  it('우승자가 여럿이면 공동 우승으로 모두의 이름을 보여 준다', () => {
    renderResult([ranked(1, '하나', 5, 1, true), ranked(2, '둘', 5, 1, true), ranked(3, '셋', 1, 3)]);

    expect(screen.getByText('공동 우승')).toBeInTheDocument();
    expect(screen.getByText('하나, 둘')).toBeInTheDocument();
  });

  it('우승자가 한 명이면 그냥 우승이다', () => {
    renderResult([ranked(1, '하나', 5, 1, true), ranked(2, '둘', 3, 2)]);

    expect(screen.getByText('우승')).toBeInTheDocument();
    expect(screen.queryByText('공동 우승')).not.toBeInTheDocument();
  });

  it('서버가 우승자를 주지 않으면 우승자 없음을 보인다', () => {
    renderResult([ranked(1, '하나', 0, 1), ranked(2, '둘', 0, 1)]);

    expect(screen.getByText('우승자 없음')).toBeInTheDocument();
    expect(screen.queryByText('공동 우승')).not.toBeInTheDocument();
  });

  it('4위부터는 배지 대신 순위 번호를 쓴다', () => {
    renderResult([
      ranked(1, '가', 9, 1, true),
      ranked(2, '나', 8, 2),
      ranked(3, '다', 7, 3),
      ranked(4, '라', 6, 4),
    ]);

    expect(within(rankRows()[3]).getByText('#4')).toBeInTheDocument();
  });
});
