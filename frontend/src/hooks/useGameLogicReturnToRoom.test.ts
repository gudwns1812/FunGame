import { renderHook, act, waitFor } from '@testing-library/react';
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
    localStorage.setItem('ums_member_id', '1');

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 0 } });
    mockedAxios.get = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/users')) {
        return Promise.resolve({
          data: {
            result: 'SUCCESS',
            data: {
              players: [{ memberId: 1, nickname: '나', isReady: true }],
              hostMemberId: 1,
              hostNickname: '나',
            },
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
        hostMemberId: 1,
        hostName: '나',
        playerCount: 1,
        maxPlayers: 8,
        status: 'WAITING',
      });
    });

    act(() => {
      publishToRoom({ type: 'CORRECT_ANSWER', memberId: 1, nickname: '나', score: 30 });
    });
    expect(result.current.players.find((player) => player.name === '나')?.score).toBe(30);

    act(() => {
      publishToRoom({ type: 'GAME_START', gameType: 'CS', category: null, songCount: 5 });
    });

    expect(result.current.players.every((player) => player.score === 0)).toBe(true);
  });

  it('이미 로그인한 채로 새로고침해도 identify 로 회원 번호를 되찾는다', async () => {
    // given: 예전 버전에서 만들어진 로컬스토리지에는 회원 번호가 없다
    localStorage.removeItem('ums_member_id');

    const { result } = renderHook(() => useGameLogic(), { wrapper: createSseStub().wrapper });

    act(() => {
      result.current.identify(1, '나');
    });

    await act(async () => {
      await result.current.joinRoom({
        id: '7',
        name: '테스트 방',
        hostMemberId: 1,
        hostName: '나',
        playerCount: 1,
        maxPlayers: 8,
        status: 'WAITING',
      });
    });

    // then: 방장 판정이 회원 번호로 이뤄진다
    expect(result.current.isHost).toBe(true);
    // 화면 상태는 identify 가 건드리지 않는다
    expect(result.current.status).toBe('WAITING');
  });

  it('방 안에서 새로고침해 회원 번호가 늦게 채워져도 방장으로 본다', async () => {
    // given: 새로고침 직후라 회원 번호는 아직 없고, 대기실 상태만 남아 있다
    localStorage.removeItem('ums_member_id');
    localStorage.setItem('ums_status', 'WAITING');
    localStorage.setItem('ums_roomId', '7');
    mockedAxios.get = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/health')) {
        return Promise.resolve({ data: { result: 'SUCCESS', data: 'ok' } });
      }
      if (url.endsWith('/users')) {
        return Promise.resolve({
          data: {
            result: 'SUCCESS',
            data: {
              players: [{ memberId: 1, nickname: '나', isReady: true }],
              hostMemberId: 1,
              hostNickname: '나',
            },
          },
        });
      }
      return Promise.resolve({ data: { result: 'SUCCESS', data: [] } });
    });

    const { result } = renderHook(() => useGameLogic(), { wrapper: createSseStub().wrapper });

    // when: 로그인 확인이 재참가보다 먼저 끝나 회원 번호가 뒤늦게 들어온다
    act(() => {
      result.current.identify(1, '나');
    });

    // then: 뒤늦게 끝난 재참가가 방장 판정을 뒤집지 않는다
    await waitFor(() => expect(result.current.isBootstrapping).toBe(false));
    expect(result.current.isHost).toBe(true);
  });

  it('닉네임이 같아도 회원 번호가 다르면 방장으로 보지 않는다', async () => {
    mockedAxios.get = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/users')) {
        return Promise.resolve({
          data: {
            result: 'SUCCESS',
            data: {
              players: [{ memberId: 1, nickname: '나', isReady: true }],
              // 방장의 닉네임은 나와 같지만 다른 회원이다
              hostMemberId: 2,
              hostNickname: '나',
            },
          },
        });
      }
      return Promise.resolve({ data: { result: 'SUCCESS', data: [] } });
    });

    const { result } = renderHook(() => useGameLogic(), { wrapper: createSseStub().wrapper });

    await act(async () => {
      await result.current.joinRoom({
        id: '7',
        name: '테스트 방',
        hostMemberId: 2,
        hostName: '나',
        playerCount: 1,
        maxPlayers: 8,
        status: 'WAITING',
      });
    });

    expect(result.current.isHost).toBe(false);
    expect(result.current.players.find((player) => player.memberId === 1)?.isHost).toBe(false);
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
