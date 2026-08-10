import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import type { StompConfig } from '@stomp/stompjs';
import { useGameLogic } from './useGameLogic';

vi.mock('axios');
vi.mock('sockjs-client');

/** new Client(config) 로 넘어간 설정들. onConnect 를 직접 호출해 재연결을 흉내낸다. */
type CapturedConfig = Required<Pick<StompConfig, 'onConnect' | 'onWebSocketClose'>>;
const clientConfigs: CapturedConfig[] = [];

vi.mock('@stomp/stompjs', () => ({
  Client: class {
    connected = true;
    activate = vi.fn();
    deactivate = vi.fn();
    subscribe = vi.fn();
    publish = vi.fn();

    constructor(config: CapturedConfig) {
      clientConfigs.push(config);
    }
  },
}));

const mockedAxios = axios as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  defaults: Partial<typeof axios.defaults>;
};

const ROOM = {
  id: '7',
  name: '테스트방',
  hostName: '방장',
  playerCount: 1,
  maxPlayers: 8,
  status: 'WAITING' as const,
};

const joinCallsFor = (roomId: string) =>
  mockedAxios.post.mock.calls.filter((call) => call[0] === `/game/rooms/${roomId}/join`);

describe('useGameLogic 재연결 시 참가 상태 동기화', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clientConfigs.length = 0;
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 0 } });
    mockedAxios.get = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/users')) {
        return Promise.resolve({
          data: {
            result: 'SUCCESS',
            data: { players: [{ name: '나', isReady: false }], host: '방장' },
          },
        });
      }
      // 방 목록 등 그 외 조회
      return Promise.resolve({ data: { result: 'SUCCESS', data: [] } });
    });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
    vi.spyOn(window, 'alert').mockImplementation(() => {});
  });

  const joinAndGetConfig = async () => {
    const { result } = renderHook(() => useGameLogic());
    await act(async () => {
      await result.current.joinRoom(ROOM);
    });
    expect(clientConfigs).toHaveLength(1);
    return { result, config: clientConfigs[0] };
  };

  it('onConnect 와 onWebSocketClose 를 모두 등록한다', async () => {
    const { config } = await joinAndGetConfig();

    expect(typeof config.onConnect).toBe('function');
    expect(typeof config.onWebSocketClose).toBe('function');
  });

  it('최초 연결에서는 join 을 다시 호출하지 않는다', async () => {
    const { config } = await joinAndGetConfig();
    expect(joinCallsFor(ROOM.id)).toHaveLength(1);

    // 최초 onConnect
    await act(async () => {
      config.onConnect(undefined as never);
    });

    expect(joinCallsFor(ROOM.id)).toHaveLength(1);
  });

  it('재연결되면 join 을 다시 호출해 방에서 빠진 상태를 복구한다', async () => {
    const { config } = await joinAndGetConfig();

    // 최초 연결 -> 끊김 -> 재연결
    await act(async () => {
      config.onConnect(undefined as never);
    });
    await act(async () => {
      config.onWebSocketClose(undefined as never);
    });
    await act(async () => {
      config.onConnect(undefined as never);
    });

    await waitFor(() => {
      expect(joinCallsFor(ROOM.id)).toHaveLength(2);
    });
    // 인원 목록도 다시 맞춘다
    expect(mockedAxios.get).toHaveBeenCalledWith(`/game/rooms/${ROOM.id}/users`);
  });

  it('재연결 후 join 이 거부되면 로비로 돌려보낸다', async () => {
    const { result, config } = await joinAndGetConfig();

    await act(async () => {
      config.onConnect(undefined as never);
    });

    mockedAxios.post = vi.fn().mockRejectedValue({
      response: { data: { error: { message: '방이 종료되었습니다.' } } },
    });

    await act(async () => {
      config.onConnect(undefined as never);
    });

    await waitFor(() => {
      expect(result.current.status).toBe('ROOM_LIST');
    });
    expect(result.current.roomId).toBeNull();
    expect(window.alert).toHaveBeenCalledWith('방이 종료되었습니다.');
  });
});
