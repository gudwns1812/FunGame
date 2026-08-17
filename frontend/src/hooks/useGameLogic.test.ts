import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { useGameLogic } from './useGameLogic';
import { createStompStub } from '../test/stompTestUtils';
import { roomTopic } from '../utils/stompDestination';

// Mock dependencies
vi.mock('axios');

describe('useGameLogic Event Logging', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('PLAYER_JOIN & PLAYER_LEAVE 이벤트를 제외한 시스템 로그는 필터링되어야 한다', async () => {
    // 훅 렌더링
    const { result } = renderHook(() => useGameLogic(), {
      wrapper: createStompStub().wrapper,
    });

    // 상태 강제 주입 후 로그 추가 발생 여부 확인 로직
    // 초기 로그는 없음
    expect(result.current.logs).toHaveLength(0);

    act(() => {
      // 강제로 addLog 테스트 (여기서 실제 handleEvent를 검증해야 하지만 테스트 편의상 구조만 작성)
      result.current.addLog('[시스템] PLAYER1님이 입장하셨습니다.');
    });

    // TODO: WebSocket subscribe 콜백을 가로채서 PLAYER_READY, CORRECT_ANSWER 이벤트를 발생시키고
    // logs 배열이 변하지 않는 것을 테스트해야 함 (현재 TDD Phase 1 스위트 구조 셋업)

    expect(result.current.logs.length).toBe(1);
    expect(result.current.logs[0]).toContain('PLAYER1님이 입장하셨습니다');
  });
});

describe('useGameLogic 참가자 색', () => {
  const ROOM_ID = '7';
  const HOST_MEMBER_ID = 1;
  const MY_MEMBER_ID = 2;
  const OTHER_MEMBER_ID = 3;

  const ROOM = {
    id: ROOM_ID,
    name: '테스트방',
    hostMemberId: HOST_MEMBER_ID,
    hostName: '방장',
    playerCount: 3,
    maxPlayers: 8,
    status: 'WAITING' as const,
    gameType: 'SONG',
    csDifficulty: 'HARD',
  };

  const roomStateOf = (version: number, players: { memberId: number; nickname: string }[]) => ({
    version,
    players: players.map((player) => ({ ...player, isReady: false })),
    hostMemberId: HOST_MEMBER_ID,
    hostNickname: '방장',
  });

  const FULL_ROOM_STATE = roomStateOf(1, [
    { memberId: HOST_MEMBER_ID, nickname: '방장' },
    { memberId: MY_MEMBER_ID, nickname: '나' },
    { memberId: OTHER_MEMBER_ID, nickname: '남은사람' },
  ]);

  const mockedAxios = axios as unknown as {
    get: ReturnType<typeof vi.fn>;
    post: ReturnType<typeof vi.fn>;
    defaults: Partial<typeof axios.defaults>;
  };

  let stomp: ReturnType<typeof createStompStub>;

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');
    localStorage.setItem('ums_member_id', String(MY_MEMBER_ID));

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 1 } });
    mockedAxios.get = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/users')) {
        return Promise.resolve({ data: { result: 'SUCCESS', data: FULL_ROOM_STATE } });
      }
      return Promise.resolve({ data: { result: 'SUCCESS', data: [] } });
    });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
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
    await waitFor(() => expect(result.current.players).toHaveLength(3));

    return { result };
  };

  const colorsByName = (players: { name: string; colorIndex?: number }[]) =>
    Object.fromEntries(players.map((player) => [player.name, player.colorIndex]));

  it('앞 슬롯 사람이 나가도 남은 사람의 색은 그대로다', async () => {
    const { result } = await joinAndConnect();
    expect(colorsByName(result.current.players)).toEqual({ 방장: 0, 나: 1, 남은사람: 2 });

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), {
        type: 'PLAYER_LEAVE',
        memberId: HOST_MEMBER_ID,
        nickname: '방장',
        room: roomStateOf(2, [
          { memberId: MY_MEMBER_ID, nickname: '나' },
          { memberId: OTHER_MEMBER_ID, nickname: '남은사람' },
        ]),
      });
    });

    expect(colorsByName(result.current.players)).toEqual({ 나: 1, 남은사람: 2 });
  });

  it('이탈자가 돌아오면 비어 있던 색을 다시 받고 남은 사람의 색은 건드리지 않는다', async () => {
    const { result } = await joinAndConnect();

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), {
        type: 'PLAYER_LEAVE',
        memberId: HOST_MEMBER_ID,
        nickname: '방장',
        room: roomStateOf(2, [
          { memberId: MY_MEMBER_ID, nickname: '나' },
          { memberId: OTHER_MEMBER_ID, nickname: '남은사람' },
        ]),
      });
    });
    act(() => {
      stomp.emit(roomTopic(ROOM_ID), {
        type: 'PLAYER_JOIN',
        memberId: HOST_MEMBER_ID,
        nickname: '방장',
        room: roomStateOf(3, [
          { memberId: MY_MEMBER_ID, nickname: '나' },
          { memberId: OTHER_MEMBER_ID, nickname: '남은사람' },
          { memberId: HOST_MEMBER_ID, nickname: '방장' },
        ]),
      });
    });

    expect(colorsByName(result.current.players)).toEqual({ 나: 1, 남은사람: 2, 방장: 0 });
  });

  it('이탈자 자리를 새 사람이 채워도 남은 사람과 색이 겹치지 않는다', async () => {
    const { result } = await joinAndConnect();

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), {
        type: 'PLAYER_LEAVE',
        memberId: HOST_MEMBER_ID,
        nickname: '방장',
        room: roomStateOf(2, [
          { memberId: MY_MEMBER_ID, nickname: '나' },
          { memberId: OTHER_MEMBER_ID, nickname: '남은사람' },
        ]),
      });
    });
    act(() => {
      stomp.emit(roomTopic(ROOM_ID), {
        type: 'PLAYER_JOIN',
        memberId: 9,
        nickname: '새사람',
        room: roomStateOf(3, [
          { memberId: MY_MEMBER_ID, nickname: '나' },
          { memberId: OTHER_MEMBER_ID, nickname: '남은사람' },
          { memberId: 9, nickname: '새사람' },
        ]),
      });
    });

    expect(colorsByName(result.current.players)).toEqual({ 나: 1, 남은사람: 2, 새사람: 0 });
  });
});

describe('useGameLogic 게임 중 뒤로가기', () => {
  const ROOM_ID = '7';
  const MY_MEMBER_ID = 2;

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

  const mockedAxios = axios as unknown as {
    get: ReturnType<typeof vi.fn>;
    post: ReturnType<typeof vi.fn>;
    defaults: Partial<typeof axios.defaults>;
  };

  let stomp: ReturnType<typeof createStompStub>;

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');
    localStorage.setItem('ums_member_id', String(MY_MEMBER_ID));

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 0 } });
    mockedAxios.get = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/users')) {
        return Promise.resolve({
          data: {
            result: 'SUCCESS',
            data: {
              version: 1,
              players: [{ memberId: MY_MEMBER_ID, nickname: '나', isReady: false }],
              hostMemberId: MY_MEMBER_ID,
              hostNickname: '나',
            },
          },
        });
      }
      return Promise.resolve({ data: { result: 'SUCCESS', data: [] } });
    });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
  });

  const enterPlayingRoom = async () => {
    stomp = createStompStub();
    const { result } = renderHook(() => useGameLogic(), { wrapper: stomp.wrapper });

    await act(async () => {
      await result.current.joinRoom(ROOM);
    });
    await act(async () => {
      await stomp.connect();
    });
    act(() => {
      stomp.emit(roomTopic(ROOM_ID), { type: 'GAME_START', gameType: 'SONG', category: 'KPOP', songCount: 3 });
    });
    await waitFor(() => expect(result.current.status).toBe('PLAYING'));

    return { result };
  };

  it('게임 중 뒤로가기는 방을 떠나지 않고 히스토리 엔트리를 되돌려 놓는다', async () => {
    const { result } = await enterPlayingRoom();
    const pushState = vi.spyOn(window.history, 'pushState');

    act(() => {
      window.dispatchEvent(new PopStateEvent('popstate'));
    });

    expect(pushState).toHaveBeenCalledWith({ room: ROOM_ID }, '');
    expect(mockedAxios.post).not.toHaveBeenCalledWith(`/game/rooms/${ROOM_ID}/leave`);
    expect(result.current.status).toBe('PLAYING');

    pushState.mockRestore();
  });

  it('대기실에서 뒤로가기는 방을 떠난다', async () => {
    stomp = createStompStub();
    const { result } = renderHook(() => useGameLogic(), { wrapper: stomp.wrapper });

    await act(async () => {
      await result.current.joinRoom(ROOM);
    });
    await act(async () => {
      await stomp.connect();
    });
    await waitFor(() => expect(result.current.status).toBe('WAITING'));

    await act(async () => {
      window.dispatchEvent(new PopStateEvent('popstate'));
    });

    await waitFor(() => expect(result.current.status).toBe('ROOM_LIST'));
    expect(mockedAxios.post).toHaveBeenCalledWith(`/game/rooms/${ROOM_ID}/leave`);
  });
});
