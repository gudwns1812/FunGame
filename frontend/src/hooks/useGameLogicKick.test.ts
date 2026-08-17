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
const OTHER_MEMBER_ID = 3;

const ROOM = {
  id: '7',
  name: '테스트방',
  hostMemberId: HOST_MEMBER_ID,
  hostName: '방장',
  playerCount: 3,
  maxPlayers: 8,
  status: 'WAITING' as const,
  gameType: 'SONG',
  csDifficulty: 'HARD',
};

const SETTINGS = {
  title: '테스트방',
  gameType: 'SONG',
  maxPlayers: 8,
  category: 'KPOP',
  totalRound: 5,
  difficulty: 0,
  hostMemberId: HOST_MEMBER_ID,
  hostNickname: '방장',
};

const usersResponse = {
  data: {
    result: 'SUCCESS',
    data: {
      players: [
        { memberId: HOST_MEMBER_ID, nickname: '방장', isReady: true },
        { memberId: MY_MEMBER_ID, nickname: '나', isReady: false },
        { memberId: OTHER_MEMBER_ID, nickname: '다른사람', isReady: false },
      ],
      hostMemberId: HOST_MEMBER_ID,
      hostNickname: '방장',
    },
  },
};

const postedUrls = () => mockedAxios.post.mock.calls.map(([url]) => url as string);

describe('useGameLogic 강퇴', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    stompClients.length = 0;
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');
    localStorage.setItem('ums_member_id', String(MY_MEMBER_ID));

    mockedAxios.get = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/users')) {
        return Promise.resolve(usersResponse);
      }
      if (url.endsWith('/settings')) {
        return Promise.resolve({ data: { result: 'SUCCESS', data: SETTINGS } });
      }
      return Promise.resolve({ data: { result: 'SUCCESS', data: [] } });
    });
    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 0 } });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
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

  it('방장이 내보내기를 누르면 강퇴할 대상을 담아 서버에 요청한다', async () => {
    const { result } = await joinRoomAndSubscribe();

    await act(async () => {
      await result.current.kickPlayer(OTHER_MEMBER_ID);
    });

    expect(mockedAxios.post).toHaveBeenCalledWith('/game/rooms/7/kick', { targetMemberId: OTHER_MEMBER_ID });
  });

  it('내가 강퇴되면 안내와 함께 방을 떠난다', async () => {
    const { result, emit } = await joinRoomAndSubscribe();

    await emit({ type: 'PLAYER_KICKED', memberId: MY_MEMBER_ID, nickname: '나' });

    await waitFor(() => expect(result.current.status).toBe('ROOM_LIST'));
    expect(result.current.roomId).toBeNull();
    expect(result.current.kickedNotice).not.toBeNull();
  });

  it('강퇴로 방을 떠날 때는 퇴장 요청을 따로 보내지 않는다', async () => {
    const { result, emit } = await joinRoomAndSubscribe();

    await emit({ type: 'PLAYER_KICKED', memberId: MY_MEMBER_ID, nickname: '나' });

    await waitFor(() => expect(result.current.status).toBe('ROOM_LIST'));
    expect(postedUrls()).not.toContain('/game/rooms/7/leave');
  });

  it('안내를 닫으면 강퇴 안내가 사라진다', async () => {
    const { result, emit } = await joinRoomAndSubscribe();

    await emit({ type: 'PLAYER_KICKED', memberId: MY_MEMBER_ID, nickname: '나' });
    await waitFor(() => expect(result.current.kickedNotice).not.toBeNull());

    act(() => {
      result.current.dismissKickedNotice();
    });

    expect(result.current.kickedNotice).toBeNull();
  });

  it('다른 사람이 강퇴되면 방에 남아 시스템 기록만 남긴다', async () => {
    const { result, emit } = await joinRoomAndSubscribe();

    await emit({ type: 'PLAYER_KICKED', memberId: OTHER_MEMBER_ID, nickname: '다른사람' });

    await waitFor(() => expect(result.current.logs.some((log) => log.includes('다른사람'))).toBe(true));
    expect(result.current.status).toBe('WAITING');
    expect(result.current.kickedNotice).toBeNull();
  });
});
