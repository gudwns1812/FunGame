import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { useGameLogic } from './useGameLogic';
import { roomTopic } from '../utils/stompDestination';
import { createStompStub } from '../test/stompTestUtils';

vi.mock('axios');

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

const roomState = (version: number, myReady: boolean) => ({
  version,
  players: [
    { memberId: HOST_MEMBER_ID, nickname: '방장', isReady: true },
    { memberId: MY_MEMBER_ID, nickname: '나', isReady: myReady },
  ],
  hostMemberId: HOST_MEMBER_ID,
  hostNickname: '방장',
});

const usersResponse = (myReady: boolean) => ({
  data: { result: 'SUCCESS', data: roomState(1, myReady) },
});

const roomStateFetchCount = () =>
  mockedAxios.get.mock.calls.filter((call) => String(call[0]).endsWith('/users')).length;

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
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');
    localStorage.setItem('ums_member_id', String(MY_MEMBER_ID));

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 0 } });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
    stubUsers(true);
  });

  const joinRoomAndSubscribe = async () => {
    const stomp = createStompStub();
    const { result } = renderHook(() => useGameLogic(), {
      wrapper: stomp.wrapper,
    });

    await act(async () => {
      await result.current.joinRoom(ROOM);
    });
    await act(async () => {
      await stomp.connect();
    });

    const emit = async (payload: unknown) => {
      await act(async () => {
        stomp.emit(roomTopic(ROOM.id), payload);
      });
    };

    return { result, emit };
  };

  it('방 설정이 바뀌면 이벤트에 실려 온 초기화된 준비 상태를 그대로 반영한다', async () => {
    const { result, emit } = await joinRoomAndSubscribe();

    await waitFor(() => expect(myReadyState(result)).toBe(true));
    const fetchesBefore = roomStateFetchCount();

    await emit({ type: 'ROOM_SETTINGS_CHANGED', settings: SETTINGS, room: roomState(2, false) });

    await waitFor(() => expect(myReadyState(result)).toBe(false));
    expect(roomStateFetchCount()).toBe(fetchesBefore);
  });

  it('자기 버전보다 낮은 방 상태는 버린다', async () => {
    const { result, emit } = await joinRoomAndSubscribe();

    await waitFor(() => expect(myReadyState(result)).toBe(true));

    await emit({ type: 'PLAYER_READY', memberId: MY_MEMBER_ID, nickname: '나', room: roomState(5, false) });
    await waitFor(() => expect(myReadyState(result)).toBe(false));

    await emit({ type: 'PLAYER_READY', memberId: MY_MEMBER_ID, nickname: '나', room: roomState(4, true) });

    expect(myReadyState(result)).toBe(false);
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
