import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { useGameLogic } from './useGameLogic';
import { createSseStub } from '../test/sseTestUtils';

vi.mock('axios');
vi.mock('sockjs-client');

type RoomMessageHandler = (message: { body: string }) => void;
const roomSubscribers: RoomMessageHandler[] = [];

vi.mock('@stomp/stompjs', () => ({
  TickerStrategy: { Interval: 'interval', Worker: 'worker' },
  Client: class {
    connected = true;
    private readonly config: { onConnect?: (frame: unknown) => void };

    constructor(config: { onConnect?: (frame: unknown) => void }) {
      this.config = config;
    }

    activate() {
      this.config.onConnect?.(undefined);
    }

    deactivate = vi.fn();
    publish = vi.fn();

    subscribe(_destination: string, handler: RoomMessageHandler) {
      roomSubscribers.push(handler);
    }
  },
}));

const publishToRoom = (event: Record<string, unknown>) => {
  const body = JSON.stringify({ result: 'SUCCESS', data: event });
  roomSubscribers.forEach((handler) => handler({ body }));
};

const mockedAxios = axios as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  defaults: Partial<typeof axios.defaults>;
};

describe('useGameLogic 결과창에서 게임방으로 돌아가기', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    roomSubscribers.length = 0;
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 0 } });
    mockedAxios.get = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/users')) {
        return Promise.resolve({
          data: {
            result: 'SUCCESS',
            data: { players: [{ name: '나', isReady: true }], host: '나' },
          },
        });
      }
      return Promise.resolve({ data: { result: 'SUCCESS', data: [] } });
    });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
  });

  it('지난 판의 로그를 지운 채로 대기실에 들어간다', async () => {
    const { result } = renderHook(() => useGameLogic(), { wrapper: createSseStub().wrapper });

    act(() => {
      result.current.addLog('[시스템] 1라운드 정답은 밤양갱');
      result.current.addLog('참가자: 정답!');
    });
    expect(result.current.logs).toHaveLength(2);

    await act(async () => {
      await result.current.returnToWaitingRoom();
    });

    expect(result.current.logs).toHaveLength(0);
    expect(result.current.status).toBe('WAITING');
  });

  it('새 판이 시작되면 지난 판의 점수를 0 으로 되돌린다', async () => {
    const { result } = renderHook(() => useGameLogic(), { wrapper: createSseStub().wrapper });

    await act(async () => {
      await result.current.joinRoom({
        id: '7',
        name: '테스트 방',
        hostName: '나',
        playerCount: 1,
        maxPlayers: 8,
        status: 'WAITING',
      });
    });

    act(() => {
      publishToRoom({ type: 'CORRECT_ANSWER', playerName: '나', score: 30 });
    });
    expect(result.current.players.find((player) => player.name === '나')?.score).toBe(30);

    act(() => {
      publishToRoom({ type: 'GAME_START', gameType: 'CS', category: null, songCount: 5 });
    });

    expect(result.current.players.every((player) => player.score === 0)).toBe(true);
  });

  it('진행 중이던 라운드 화면 상태도 함께 비운다', async () => {
    const { result } = renderHook(() => useGameLogic(), { wrapper: createSseStub().wrapper });

    await act(async () => {
      await result.current.returnToWaitingRoom();
    });

    expect(result.current.hint).toBe('');
    expect(result.current.currentVideoId).toBe('');
    expect(localStorage.getItem('ums_currentVideoId')).toBeNull();
  });
});
