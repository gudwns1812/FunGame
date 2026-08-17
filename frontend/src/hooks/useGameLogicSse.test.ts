import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { useGameLogic } from './useGameLogic';
import { createSseStub } from '../test/sseTestUtils';
import { createStompStub, nestWrappers } from '../test/stompTestUtils';

vi.mock('axios');

const mockedAxios = axios as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  defaults: Partial<typeof axios.defaults>;
};

const roomListFetchCount = () =>
  mockedAxios.get.mock.calls.filter((call) => call[0] === '/game/rooms').length;

const setTabVisibility = (state: DocumentVisibilityState) => {
  Object.defineProperty(document, 'visibilityState', {
    configurable: true,
    get: () => state,
  });
  document.dispatchEvent(new Event('visibilitychange'));
};

const DEBOUNCE_SETTLE_MS = 400;

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

const letDebounceSettle = () =>
  act(async () => {
    await new Promise((resolve) => setTimeout(resolve, DEBOUNCE_SETTLE_MS));
  });

describe('useGameLogic 로비 방 목록 동기화', () => {
  let sse: ReturnType<typeof createSseStub>;

  const renderInLobby = () =>
    renderHook(() => useGameLogic(), { wrapper: nestWrappers(sse.wrapper, createStompStub().wrapper) });

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 0 } });
    mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: [] } });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };

    sse = createSseStub();
    setTabVisibility('visible');
  });

  it('로비에 진입하면 방 목록을 한 번만 가져온다', async () => {
    renderInLobby();

    await letDebounceSettle();

    expect(roomListFetchCount()).toBe(1);
  });

  it('로비에 진입하면 방 변경 알림을 구독한다', async () => {
    renderInLobby();

    await waitFor(() => expect(sse.listenerCountOf('room-update')).toBe(1));
  });

  it('방 변경 알림에 실린 목록으로 갱신한다', async () => {
    const { result } = renderInLobby();
    await letDebounceSettle();

    act(() => sse.emit('room-update', JSON.stringify([PUSHED_ROOM])));
    await letDebounceSettle();

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
    await letDebounceSettle();
    const fetchesBefore = roomListFetchCount();

    act(() => sse.emit('room-update', JSON.stringify([PUSHED_ROOM])));
    await letDebounceSettle();

    expect(roomListFetchCount()).toBe(fetchesBefore);
  });

  it('해석할 수 없는 알림을 받으면 목록을 다시 가져와 보정한다', async () => {
    renderInLobby();
    await letDebounceSettle();
    const fetchesBefore = roomListFetchCount();

    act(() => sse.emit('room-update', 'REFRESH'));
    await letDebounceSettle();

    expect(roomListFetchCount()).toBe(fetchesBefore + 1);
  });

  it('끊겼다 다시 연결되면 놓친 방 변경을 보정한다', async () => {
    renderInLobby();
    await letDebounceSettle();
    const fetchesBefore = roomListFetchCount();

    act(() => sse.emit('connected', 'Connected'));
    await letDebounceSettle();

    expect(roomListFetchCount()).toBe(fetchesBefore + 1);
  });

  it('탭으로 돌아오면 놓친 방 변경을 보정하려고 목록을 다시 가져온다', async () => {
    renderInLobby();
    await letDebounceSettle();
    const fetchesBefore = roomListFetchCount();

    act(() => setTabVisibility('visible'));
    await letDebounceSettle();

    expect(roomListFetchCount()).toBe(fetchesBefore + 1);
  });

  it('탭이 백그라운드로 가는 시점에는 아무것도 하지 않는다', async () => {
    renderInLobby();
    await letDebounceSettle();
    const fetchesBefore = roomListFetchCount();

    act(() => setTabVisibility('hidden'));
    await letDebounceSettle();

    expect(roomListFetchCount()).toBe(fetchesBefore);
  });

  it('로비를 벗어나면 방 변경 구독을 정리한다', async () => {
    const { unmount } = renderInLobby();
    await waitFor(() => expect(sse.listenerCountOf('room-update')).toBe(1));

    unmount();

    expect(sse.listenerCountOf('room-update')).toBe(0);
    expect(sse.listenerCountOf('connected')).toBe(0);
  });
});
