import { render, screen, within } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import MyGameRecordsSection from './MyGameRecordsSection';
import type { GameRecord } from '../../types/record';

vi.mock('axios');

const mockedAxios = axios as unknown as { get: ReturnType<typeof vi.fn> };

const RECORDS: GameRecord[] = [
  { gameType: 'SONG', played: 3, won: 2, lost: 1, winRate: 67 },
  { gameType: 'CS', played: 0, won: 0, lost: 0, winRate: null },
  { gameType: 'HANGMAN', played: 1, won: 0, lost: 1, winRate: 0 },
];

const givenRecords = (records: GameRecord[]) => {
  mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: records } });
};

describe('MyGameRecordsSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('게임마다 카드를 따로 두고 방 만들 때와 같은 이름으로 보여준다', async () => {
    givenRecords(RECORDS);

    render(<MyGameRecordsSection />);

    const cards = await screen.findAllByRole('region');
    expect(cards.map((card) => card.getAttribute('aria-label'))).toEqual([
      '음악 퀴즈 전적',
      'CS 퀴즈 전적',
      '행맨 전적',
    ]);
  });

  it('한 게임의 판수와 승패와 승률을 보여준다', async () => {
    givenRecords(RECORDS);

    render(<MyGameRecordsSection />);

    const song = await screen.findByRole('region', { name: '음악 퀴즈 전적' });
    expect(within(song).getByText('3판')).toBeInTheDocument();
    expect(within(song).getByText('2승 1패')).toBeInTheDocument();
    expect(within(song).getByText('67%')).toBeInTheDocument();
  });

  it('한 판도 안 한 게임은 승률 대신 아직 판이 없다고 알려준다', async () => {
    givenRecords(RECORDS);

    render(<MyGameRecordsSection />);

    const cs = await screen.findByRole('region', { name: 'CS 퀴즈 전적' });
    expect(within(cs).getByText('아직 플레이한 판이 없습니다.')).toBeInTheDocument();
    expect(within(cs).queryByText(/%/)).not.toBeInTheDocument();
  });

  it('게임을 합친 숫자나 서버 내부 코드는 보여주지 않는다', async () => {
    givenRecords(RECORDS);

    const { container } = render(<MyGameRecordsSection />);
    await screen.findAllByRole('region');

    expect(container).not.toHaveTextContent(/SONG|HANGMAN|NONE/);
    expect(container).not.toHaveTextContent(/합계|전체|총/);
    expect(container).not.toHaveTextContent('4판');
  });

  it('불러오지 못하면 그렇다고 알려준다', async () => {
    mockedAxios.get = vi.fn().mockRejectedValue(new Error('network'));

    render(<MyGameRecordsSection />);

    expect(await screen.findByText('전적을 불러오지 못했습니다.')).toBeInTheDocument();
  });
});
