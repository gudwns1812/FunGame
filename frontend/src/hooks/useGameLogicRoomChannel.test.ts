import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import type { StompConfig } from '@stomp/stompjs';
import { useGameLogic } from './useGameLogic';
import { createSseStub } from '../test/sseTestUtils';

vi.mock('axios');
vi.mock('sockjs-client');

/** join API · SUBSCRIBE · 스냅샷 조회가 실제로 불린 순서 */
const traced: string[] = [];

const stompClients: Array<{
  config: Required<Pick<StompConfig, 'onConnect'>>;
  subscribe: ReturnType<typeof vi.fn>;
}> = [];

vi.mock('@stomp/stompjs', () => ({
  TickerStrategy: { Interval: 'interval', Worker: 'worker' },
  Client: class {
    connected = true;
    active = true;
    config: unknown;
    activate = vi.fn();
    deactivate = vi.fn().mockResolvedValue(undefined);
    publish = vi.fn();
    subscribe = vi.fn((destination: string) => {
      traced.push(`subscribe ${destination}`);
    });

    constructor(config: unknown) {
      this.config = config;
      stompClients.push(this as never);
    }
  },
}));

const mockedAxios = axios as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  defaults: Partial<typeof axios.defaults>;
};

const HOST_MEMBER_ID = 1;
const MY_MEMBER_ID = 2;

const ROOM = {
  id: '7',
  name: '테스트방',
  hostMemberId: HOST_MEMBER_ID,
  hostName: '방장',
  playerCount: 2,
  maxPlayers: 8,
  status: 'WAITING' as const,
  gameType: 'SONG',
  csDifficulty: 'HARD',
};

const roomState = {
  version: 3,
  players: [
    { memberId: HOST_MEMBER_ID, nickname: '방장', isReady: true },
    { memberId: MY_MEMBER_ID, nickname: '나', isReady: false },
  ],
  hostMemberId: HOST_MEMBER_ID,
  hostNickname: '방장',
};

describe('useGameLogic 방 채널 열기', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    traced.length = 0;
    stompClients.length = 0;
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');
    localStorage.setItem('ums_member_id', String(MY_MEMBER_ID));

    mockedAxios.post = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/join')) {
        traced.push('join');
      }
      return Promise.resolve({ data: { result: 'SUCCESS', data: 0 } });
    });
    mockedAxios.get = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/users')) {
        traced.push('snapshot');
        return Promise.resolve({ data: { result: 'SUCCESS', data: roomState } });
      }
      return Promise.resolve({ data: { result: 'SUCCESS', data: [] } });
    });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
    vi.spyOn(window, 'alert').mockImplementation(() => {});
  });

  const joinAndConnect = async () => {
    const { result } = renderHook(() => useGameLogic(), { wrapper: createSseStub().wrapper });

    await act(async () => {
      await result.current.joinRoom(ROOM);
    });

    const client = stompClients[stompClients.length - 1];
    await act(async () => {
      (client.config as { onConnect: () => void }).onConnect();
    });

    return { result, client };
  };

  it('구독하기 전에 join 으로 소속을 확정하고, 구독한 뒤에 스냅샷을 읽는다', async () => {
    const { client } = await joinAndConnect();

    await act(async () => {
      (client.config as { onConnect: () => void }).onConnect();
    });

    await waitFor(() => expect(traced.filter((call) => call === 'join')).toHaveLength(2));
    expect(traced).toEqual([
      'join',
      `subscribe /topic/room/${ROOM.id}`,
      'snapshot',
      'join',
      `subscribe /topic/room/${ROOM.id}`,
      'snapshot',
    ]);
  });

  it('최초 연결에서는 방금 한 join 을 다시 하지 않고 구독부터 한다', async () => {
    await joinAndConnect();

    await waitFor(() => expect(traced).toContain('snapshot'));
    expect(traced).toEqual(['join', `subscribe /topic/room/${ROOM.id}`, 'snapshot']);
  });

  it('스냅샷으로 읽은 방 상태를 화면에 반영한다', async () => {
    const { result } = await joinAndConnect();

    await waitFor(() =>
      expect(result.current.players.map((player) => player.name)).toEqual(['방장', '나']),
    );
  });

  it('방을 떠날 때는 구독을 먼저 끊고 그 다음 leave 를 보낸다', async () => {
    const { result, client } = await joinAndConnect();
    const deactivate = (client as unknown as { deactivate: ReturnType<typeof vi.fn> }).deactivate;

    await act(async () => {
      await result.current.leaveRoom();
    });

    expect(deactivate).toHaveBeenCalledWith({ force: true });
    expect(deactivate.mock.invocationCallOrder[0]).toBeLessThan(
      mockedAxios.post.mock.invocationCallOrder[mockedAxios.post.mock.calls.length - 1],
    );
    expect(mockedAxios.post).toHaveBeenLastCalledWith(`/game/rooms/${ROOM.id}/leave`);
  });
});
