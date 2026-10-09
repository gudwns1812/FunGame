import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { useGameLogic } from './useGameLogic';
import { createStompStub } from '../test/stompTestUtils';
import { roomTopic } from '../utils/stompDestination';

vi.mock('axios');

const ROOM_ID = '7';
const MY_MEMBER_ID = 2;
const OTHER_MEMBER_ID = 3;

const ROOM = {
  id: ROOM_ID,
  name: '테스트방',
  hostMemberId: MY_MEMBER_ID,
  hostName: '나',
  playerCount: 2,
  maxPlayers: 8,
  status: 'WAITING' as const,
  gameType: 'SONG',
  csDifficulty: 'HARD',
};

const ROOM_STATE = {
  version: 1,
  players: [
    { memberId: MY_MEMBER_ID, nickname: '나', isReady: true },
    { memberId: OTHER_MEMBER_ID, nickname: '너', isReady: true },
  ],
  hostMemberId: MY_MEMBER_ID,
  hostNickname: '나',
};

const SERVER_RANKS = [
  { memberId: MY_MEMBER_ID, nickname: '나', score: 2, rank: 1 },
  { memberId: OTHER_MEMBER_ID, nickname: '너', score: 2, rank: 1 },
];

describe('useGameLogic 순위는 서버가 준 값을 그대로 쓴다', () => {
  const mockedAxios = axios as unknown as {
    get: ReturnType<typeof vi.fn>;
    post: ReturnType<typeof vi.fn>;
    defaults: Partial<typeof axios.defaults>;
  };

  let stomp: ReturnType<typeof createStompStub>;

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');
    localStorage.setItem('ums_member_id', String(MY_MEMBER_ID));

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 1 } });
    mockedAxios.get = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/users')) {
        return Promise.resolve({ data: { result: 'SUCCESS', data: ROOM_STATE } });
      }
      if (url.endsWith('/play/rank')) {
        return Promise.resolve({ data: { result: 'SUCCESS', data: SERVER_RANKS } });
      }
      return Promise.resolve({ data: { result: 'SUCCESS', data: [] } });
    });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
  });

  const joinAndConnect = async () => {
    stomp = createStompStub();
    const { result } = renderHook(() => useGameLogic(), { wrapper: stomp.wrapper });

    await act(async () => {
      await result.current.joinRoom(ROOM);
    });
    await act(async () => {
      await stomp.connect();
    });

    return { result };
  };

  const rankOf = (players: { memberId: number; rank?: number }[], memberId: number) =>
    players.find((player) => player.memberId === memberId)?.rank;

  it('라운드가 끝나면 서버 순위를 받아 담는다', async () => {
    const { result } = await joinAndConnect();

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), {
        type: 'ROUND_END',
        answer: '정답',
        winnerMemberId: MY_MEMBER_ID,
        winnerNickname: '나',
      });
    });

    await waitFor(() => expect(rankOf(result.current.players, OTHER_MEMBER_ID)).toBe(1));
    expect(rankOf(result.current.players, MY_MEMBER_ID)).toBe(1);
  });

  it('게임 결과의 순위와 우승 여부를 그대로 담는다', async () => {
    const { result } = await joinAndConnect();

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), {
        type: 'GAME_RESULT',
        rankings: [
          { memberId: MY_MEMBER_ID, nickname: '나', score: 0, rank: 1, winner: false },
          { memberId: OTHER_MEMBER_ID, nickname: '너', score: 0, rank: 1, winner: false },
        ],
      });
    });

    await waitFor(() => expect(result.current.status).toBe('RESULT'));
    expect(result.current.players.map(({ memberId, rank, winner }) => ({ memberId, rank, winner }))).toEqual([
      { memberId: MY_MEMBER_ID, rank: 1, winner: false },
      { memberId: OTHER_MEMBER_ID, rank: 1, winner: false },
    ]);
  });

  it('새 게임이 시작하면 지난 순위를 지운다', async () => {
    const { result } = await joinAndConnect();
    act(() => {
      stomp.emit(roomTopic(ROOM_ID), { type: 'ROUND_END', answer: '정답', winnerMemberId: null });
    });
    await waitFor(() => expect(rankOf(result.current.players, MY_MEMBER_ID)).toBe(1));

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), { type: 'GAME_START', gameType: 'SONG', remainingMillis: 3000 });
    });

    await waitFor(() => expect(rankOf(result.current.players, MY_MEMBER_ID)).toBeUndefined());
  });
});
