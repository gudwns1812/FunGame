import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import type { StompConfig } from '@stomp/stompjs';
import { useGameLogic } from './useGameLogic';
import { createSseStub } from '../test/sseTestUtils';

vi.mock('axios');
vi.mock('sockjs-client');

type RoomMessage = { body: string };
type RoomSubscriber = (message: RoomMessage) => void;

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
    subscribe = vi.fn();
    publish = vi.fn();

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

const SETTINGS = {
  title: '테스트방',
  gameType: 'CS',
  maxPlayers: 8,
  category: 'DEFAULT',
  totalRound: 5,
  difficulty: 0,
  hostMemberId: HOST_MEMBER_ID,
  hostNickname: '방장',
};

const usersResponse = (myReady: boolean) => ({
  data: {
    result: 'SUCCESS',
    data: {
      players: [
        { memberId: HOST_MEMBER_ID, nickname: '방장', isReady: true },
        { memberId: MY_MEMBER_ID, nickname: '나', isReady: myReady },
      ],
      hostMemberId: HOST_MEMBER_ID,
      hostNickname: '방장',
    },
  },
});

const stubUsers = (myReady: boolean) => {
  mockedAxios.get = vi.fn().mockImplementation((url: string) => {
    if (url.endsWith('/users')) {
      return Promise.resolve(usersResponse(myReady));
    }
    if (url.endsWith('/settings')) {
      return Promise.resolve({ data: { result: 'SUCCESS', data: SETTINGS } });
    }
    return Promise.resolve({ data: { result: 'SUCCESS', data: [] } });
  });
};

const myReadyState = (result: { current: ReturnType<typeof useGameLogic> }) =>
  result.current.players.find((player) => player.memberId === MY_MEMBER_ID)?.isReady;

describe('useGameLogic 준비 상태', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    stompClients.length = 0;
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');
    localStorage.setItem('ums_member_id', String(MY_MEMBER_ID));

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 0 } });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
    stubUsers(true);
    vi.spyOn(window, 'alert').mockImplementation(() => {});
  });

  const joinRoomAndSubscribe = async () => {
    const { result } = renderHook(() => useGameLogic(), { wrapper: createSseStub().wrapper });

    await act(async () => {
      await result.current.joinRoom(ROOM);
    });

    const client = stompClients[stompClients.length - 1];
    await act(async () => {
      (client.config as { onConnect: () => void }).onConnect();
    });

    const subscriber = client.subscribe.mock.calls[0][1] as RoomSubscriber;
    const emit = async (payload: unknown) => {
      await act(async () => {
        subscriber({ body: JSON.stringify({ result: 'SUCCESS', data: payload }) });
      });
    };

    return { result, emit };
  };

  it('방 설정이 바뀌면 서버가 초기화한 준비 상태를 다시 읽어온다', async () => {
    const { result, emit } = await joinRoomAndSubscribe();

    await waitFor(() => expect(myReadyState(result)).toBe(true));

    stubUsers(false);
    await emit({ type: 'ROOM_SETTINGS_CHANGED', settings: SETTINGS });

    await waitFor(() => expect(myReadyState(result)).toBe(false));
  });

  it('화면이 준비됨으로 보여도 서버가 알려준 준비 상태를 그대로 반영한다', async () => {
    const { result } = await joinRoomAndSubscribe();

    await waitFor(() => expect(myReadyState(result)).toBe(true));

    mockedAxios.post = vi.fn().mockResolvedValue({
      data: { result: 'SUCCESS', data: { memberId: MY_MEMBER_ID, ready: true, isAllReady: true } },
    });

    await act(async () => {
      await result.current.toggleReady();
    });

    expect(myReadyState(result)).toBe(true);
  });
});
