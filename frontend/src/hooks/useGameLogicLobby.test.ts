import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { useGameLogic } from './useGameLogic';
import { createStompStub } from '../test/stompTestUtils';
import { LOBBY_TOPIC } from '../utils/stompDestination';

vi.mock('axios');

const mockedAxios = axios as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  defaults: Partial<typeof axios.defaults>;
};

const roomListFetchCount = () => mockedAxios.get.mock.calls.filter((call) => call[0] === '/game/rooms').length;

const PUSHED_ROOM = {
  roomId: 7,
  title: '알림으로 온 방',
  hostMemberId: 3,
  hostNickname: '방장',
  status: 'WAITING',
  maxPlayers: 8,
  currentPlayers: 2,
  gameType: 'SONG',
  csDifficulty: 'HARD',
};

describe('useGameLogic 로비 방 목록 동기화', () => {
  let stomp: ReturnType<typeof createStompStub>;

  const renderInLobby = () => {
    stomp = createStompStub();
    return renderHook(() => useGameLogic(), { wrapper: stomp.wrapper });
  };

  const connect = () =>
    act(async () => {
      await stomp.connect();
    });

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 0 } });
    mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: [] } });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
  });

  it('연결이 맺어지면 로비를 구독하고 방 목록을 한 번 가져온다', async () => {
    renderInLobby();

    await connect();

    expect(stomp.subscriberCountOf(LOBBY_TOPIC)).toBe(1);
    expect(roomListFetchCount()).toBe(1);
  });

  it('방 변경 알림에 실린 목록으로 갱신한다', async () => {
    const { result } = renderInLobby();
    await connect();

    act(() => stomp.emit(LOBBY_TOPIC, [PUSHED_ROOM]));

    expect(result.current.rooms).toEqual([
      {
        id: PUSHED_ROOM.roomId,
        name: PUSHED_ROOM.title,
        hostMemberId: PUSHED_ROOM.hostMemberId,
        hostName: PUSHED_ROOM.hostNickname,
        playerCount: PUSHED_ROOM.currentPlayers,
        maxPlayers: PUSHED_ROOM.maxPlayers,
        status: PUSHED_ROOM.status,
        gameType: PUSHED_ROOM.gameType,
        csDifficulty: PUSHED_ROOM.csDifficulty,
      },
    ]);
  });

  it('알림에 목록이 실려 오면 다시 가져오지 않는다', async () => {
    renderInLobby();
    await connect();
    const fetchesBefore = roomListFetchCount();

    act(() => stomp.emit(LOBBY_TOPIC, [PUSHED_ROOM]));

    expect(roomListFetchCount()).toBe(fetchesBefore);
  });

  it('해석할 수 없는 알림을 받으면 목록을 다시 가져와 보정한다', async () => {
    renderInLobby();
    await connect();
    const fetchesBefore = roomListFetchCount();

    await act(async () => {
      stomp.emit(LOBBY_TOPIC, 'REFRESH');
    });

    expect(roomListFetchCount()).toBe(fetchesBefore + 1);
  });

  it('끊겼다 다시 연결되면 구독을 다시 걸고 놓친 방 변경을 보정한다', async () => {
    renderInLobby();
    await connect();
    const fetchesBefore = roomListFetchCount();

    await connect();

    expect(stomp.subscriberCountOf(LOBBY_TOPIC)).toBe(1);
    expect(roomListFetchCount()).toBe(fetchesBefore + 1);
  });

  it('로비를 벗어나면 로비 구독을 정리한다', async () => {
    const { unmount } = renderInLobby();
    await connect();
    await waitFor(() => expect(stomp.subscriberCountOf(LOBBY_TOPIC)).toBe(1));

    unmount();

    expect(stomp.subscriberCountOf(LOBBY_TOPIC)).toBe(0);
  });
});
