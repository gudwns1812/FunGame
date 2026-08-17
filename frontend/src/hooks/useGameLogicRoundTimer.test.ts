import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import axios from 'axios';
import { useGameLogic } from './useGameLogic';
import { createStompStub } from '../test/stompTestUtils';
import { roomTopic } from '../utils/stompDestination';

vi.mock('axios');

const ROOM_ID = '7';
const MY_MEMBER_ID = 2;
const ROUND_MILLIS = 30_000;

const ROOM = {
  id: ROOM_ID,
  name: '테스트방',
  hostMemberId: MY_MEMBER_ID,
  hostName: '나',
  playerCount: 1,
  maxPlayers: 8,
  status: 'WAITING' as const,
  gameType: 'SONG',
  csDifficulty: 'HARD',
};

const ROOM_STATE = {
  version: 1,
  players: [{ memberId: MY_MEMBER_ID, nickname: '나', isReady: false }],
  hostMemberId: MY_MEMBER_ID,
  hostNickname: '나',
};

const roundStart = (remainingMillis: number) => ({
  type: 'ROUND_START',
  round: 1,
  totalRound: 3,
  content: 'video-id',
  remainingMillis,
});

describe('useGameLogic 라운드 남은 시간', () => {
  const mockedAxios = axios as unknown as {
    get: ReturnType<typeof vi.fn>;
    post: ReturnType<typeof vi.fn>;
    defaults: Partial<typeof axios.defaults>;
  };

  let stomp: ReturnType<typeof createStompStub>;
  let playState: Record<string, unknown>;

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');
    localStorage.setItem('ums_member_id', String(MY_MEMBER_ID));

    playState = {
      gameType: 'SONG',
      category: 'KPOP',
      totalCount: 3,
      currentRound: 2,
      totalRound: 3,
      content: 'video-id',
      statusData: null,
      remainingMillis: 12_000,
    };

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 1 } });
    mockedAxios.get = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/users')) {
        return Promise.resolve({ data: { result: 'SUCCESS', data: ROOM_STATE } });
      }
      if (url.endsWith('/play/state')) {
        return Promise.resolve({ data: { result: 'SUCCESS', data: playState } });
      }
      return Promise.resolve({ data: { result: 'SUCCESS', data: [] } });
    });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };

    vi.useFakeTimers({ shouldAdvanceTime: true });
  });

  afterEach(() => {
    vi.useRealTimers();
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

  const advance = async (millis: number) => {
    await act(async () => {
      await vi.advanceTimersByTimeAsync(millis);
    });
  };

  it('ROUND_START 가 알려준 남은 시간부터 클라이언트가 센다', async () => {
    const { result } = await joinAndConnect();

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), roundStart(ROUND_MILLIS));
    });

    await waitFor(() => expect(result.current.timeLeft).toBe(30));
    expect(result.current.totalTime).toBe(30);
  });

  it('서버가 아무것도 더 보내지 않아도 시간이 흐르면 줄어든다', async () => {
    const { result } = await joinAndConnect();

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), roundStart(ROUND_MILLIS));
    });
    await waitFor(() => expect(result.current.timeLeft).toBe(30));

    await advance(5_000);
    expect(result.current.timeLeft).toBe(25);

    await advance(15_000);
    expect(result.current.timeLeft).toBe(10);
  });

  it('라운드 길이를 넘겨도 0 아래로 내려가지 않고 0에서 ROUND_END 를 기다린다', async () => {
    const { result } = await joinAndConnect();

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), roundStart(ROUND_MILLIS));
    });
    await waitFor(() => expect(result.current.timeLeft).toBe(30));

    await advance(ROUND_MILLIS + 5_000);

    expect(result.current.timeLeft).toBe(0);
    expect(result.current.roundEndInfo).toBeNull();
  });

  it('라운드가 시간을 다 써서 끝나면 0 을 적어두고 멈춘다', async () => {
    const { result } = await joinAndConnect();

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), roundStart(ROUND_MILLIS));
    });
    await waitFor(() => expect(result.current.timeLeft).toBe(30));

    await advance(ROUND_MILLIS);
    act(() => {
      stomp.emit(roomTopic(ROOM_ID), {
        type: 'ROUND_END',
        answer: '정답',
        winnerMemberId: null,
        winnerNickname: null,
      });
    });

    expect(result.current.timeLeft).toBe(0);
  });

  it('탭이 숨어 카운터가 멈춰 있었어도 다시 보이는 순간 기준점에서 맞춘다', async () => {
    const { result } = await joinAndConnect();

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), roundStart(ROUND_MILLIS));
    });
    await waitFor(() => expect(result.current.timeLeft).toBe(30));

    // 백그라운드 탭처럼 인터벌은 한 번도 돌지 않고 시계만 흐른 상태
    vi.setSystemTime(Date.now() + 9_000);
    expect(result.current.timeLeft).toBe(30);

    act(() => {
      document.dispatchEvent(new Event('visibilitychange'));
    });

    expect(result.current.timeLeft).toBe(21);
  });

  it('정답으로 라운드가 조기 종료되면 카운트가 그 자리에서 멈춘다', async () => {
    const { result } = await joinAndConnect();

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), roundStart(ROUND_MILLIS));
    });
    await waitFor(() => expect(result.current.timeLeft).toBe(30));
    await advance(8_000);
    expect(result.current.timeLeft).toBe(22);

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), {
        type: 'ROUND_END',
        answer: '정답',
        winnerMemberId: MY_MEMBER_ID,
        winnerNickname: '나',
      });
    });

    await advance(5_000);
    expect(result.current.timeLeft).toBe(22);
  });

  it('라운드 도중에 다시 들어오면 스냅샷이 알려준 남은 시간부터 센다', async () => {
    localStorage.setItem('ums_status', 'PLAYING');
    localStorage.setItem('ums_roomId', ROOM_ID);

    stomp = createStompStub();
    const { result } = renderHook(() => useGameLogic(), { wrapper: stomp.wrapper });

    await act(async () => {
      await stomp.connect();
    });

    await waitFor(() => expect(result.current.timeLeft).toBe(12));

    await advance(4_000);
    expect(result.current.timeLeft).toBe(8);
  });

  it('라운드 사이에 들어오면 남은 시간이 없으므로 세지 않는다', async () => {
    playState = { ...playState, remainingMillis: 0 };
    localStorage.setItem('ums_status', 'PLAYING');
    localStorage.setItem('ums_roomId', ROOM_ID);

    stomp = createStompStub();
    const { result } = renderHook(() => useGameLogic(), { wrapper: stomp.wrapper });

    await act(async () => {
      await stomp.connect();
    });
    await waitFor(() => expect(result.current.currentRound).toBe(2));

    await advance(4_000);
    expect(result.current.timeLeft).toBe(30);
  });
});
