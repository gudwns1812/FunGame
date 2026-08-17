import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { useGameLogic } from './useGameLogic';
import { createStompStub } from '../test/stompTestUtils';
import { roomTopic } from '../utils/stompDestination';

vi.mock('axios');

/** join API · SUBSCRIBE · 스냅샷 조회가 실제로 불린 순서 */
const traced: string[] = [];

const mockedAxios = axios as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  defaults: Partial<typeof axios.defaults>;
};

const HOST_MEMBER_ID = 1;
const MY_MEMBER_ID = 2;
const ROOM_ID = '7';

const ROOM = {
  id: ROOM_ID,
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
  let stomp: ReturnType<typeof createStompStub>;

  const renderGameLogic = () => {
    stomp = createStompStub({ onSubscribe: (destination) => traced.push(`subscribe ${destination}`) });
    return renderHook(() => useGameLogic(), {
      wrapper: stomp.wrapper,
    });
  };

  beforeEach(() => {
    vi.clearAllMocks();
    traced.length = 0;
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
    const { result } = renderGameLogic();

    await act(async () => {
      await result.current.joinRoom(ROOM);
    });
    await act(async () => {
      await stomp.connect();
    });

    return { result };
  };

  const reconnect = async () => {
    await act(async () => {
      await stomp.connect();
    });
  };

  it('최초 연결에서는 방금 한 join 을 다시 하지 않고 구독부터 한다', async () => {
    await joinAndConnect();

    expect(traced).toEqual(['join', `subscribe ${roomTopic(ROOM_ID)}`, 'snapshot']);
  });

  it('재연결되면 구독하기 전에 join 으로 소속을 되찾고, 구독한 뒤에 스냅샷을 읽는다', async () => {
    await joinAndConnect();
    traced.length = 0;

    await reconnect();

    expect(traced).toEqual(['join', `subscribe ${roomTopic(ROOM_ID)}`, 'snapshot']);
  });

  it('스냅샷으로 읽은 방 상태를 화면에 반영한다', async () => {
    const { result } = await joinAndConnect();

    await waitFor(() => expect(result.current.players.map((player) => player.name)).toEqual(['방장', '나']));
  });

  it('구독한 방 토픽으로 온 이벤트를 처리한다', async () => {
    const { result } = await joinAndConnect();

    act(() => {
      stomp.emit(roomTopic(ROOM_ID), { type: 'CHAT', memberId: HOST_MEMBER_ID, nickname: '방장', message: '안녕' });
    });

    expect(result.current.logs).toContain('방장: 안녕');
  });

  it('재연결 후 join 이 거부되면 로비로 돌려보낸다', async () => {
    const { result } = await joinAndConnect();

    mockedAxios.post = vi.fn().mockRejectedValue({
      response: { data: { error: { message: '방이 종료되었습니다.' } } },
    });

    await reconnect();

    await waitFor(() => expect(result.current.status).toBe('ROOM_LIST'));
    expect(result.current.roomId).toBeNull();
    expect(window.alert).toHaveBeenCalledWith('방이 종료되었습니다.');
  });

  it('방을 떠날 때는 구독을 먼저 끊고 그 다음 leave 를 보낸다', async () => {
    const { result } = await joinAndConnect();

    let subscribersWhenLeaving = -1;
    mockedAxios.post = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/leave')) {
        subscribersWhenLeaving = stomp.subscriberCountOf(roomTopic(ROOM_ID));
      }
      return Promise.resolve({ data: { result: 'SUCCESS' } });
    });

    await act(async () => {
      await result.current.leaveRoom();
    });

    expect(mockedAxios.post).toHaveBeenCalledWith(`/game/rooms/${ROOM_ID}/leave`);
    expect(subscribersWhenLeaving).toBe(0);
  });

  it('방을 떠나면 그 뒤에 온 방 이벤트는 받지 않는다', async () => {
    const { result } = await joinAndConnect();

    await act(async () => {
      await result.current.leaveRoom();
    });
    act(() => {
      stomp.emit(roomTopic(ROOM_ID), { type: 'CHAT', memberId: HOST_MEMBER_ID, nickname: '방장', message: '안녕' });
    });

    expect(result.current.logs).not.toContain('방장: 안녕');
  });
});
