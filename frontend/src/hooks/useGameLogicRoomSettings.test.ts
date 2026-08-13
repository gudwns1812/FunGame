import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { useGameLogic } from './useGameLogic';
import { createSseStub } from '../test/sseTestUtils';

vi.mock('axios');
vi.mock('sockjs-client');

const mockedAxios = axios as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  patch: ReturnType<typeof vi.fn>;
  defaults: Partial<typeof axios.defaults>;
};

const ROOM_ID = '7';

const settingsResponse = (maxPlayers: number) => ({
  data: {
    result: 'SUCCESS',
    data: {
      title: '테스트 방',
      gameType: 'CS',
      maxPlayers,
      category: 'DEFAULT',
      totalRound: 5,
      difficulty: 0,
      csDifficulty: 'HARD',
      hostMemberId: 1,
      hostNickname: '나',
    },
  },
});

describe('useGameLogic 방 설정 변경', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');
    localStorage.setItem('ums_roomId', ROOM_ID);

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 0 } });
    mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: [] } });
    mockedAxios.patch = vi.fn().mockResolvedValue(settingsResponse(4));
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
    vi.spyOn(window, 'alert').mockImplementation(() => {});
  });

  it('정원을 바꾸면 대기실 슬롯 기준 정원도 따라간다', async () => {
    const { result } = renderHook(() => useGameLogic(), { wrapper: createSseStub().wrapper });

    await act(async () => {
      await result.current.changeRoomSettings({
        gameType: 'CS',
        maxPlayers: 4,
        category: 'DEFAULT',
        totalRound: 5,
        difficulty: 0,
        csDifficulty: 'HARD',
      });
    });

    expect(result.current.roomMaxPlayers).toBe(4);
    expect(result.current.roomSettings?.maxPlayers).toBe(4);
  });

  it('설정 변경에 실패하면 서버 메시지를 알리고 정원을 바꾸지 않는다', async () => {
    mockedAxios.patch = vi.fn().mockRejectedValue({
      response: { data: { error: { message: '방이 가득찼습니다.' } } },
    });

    const { result } = renderHook(() => useGameLogic(), { wrapper: createSseStub().wrapper });
    const before = result.current.roomMaxPlayers;

    await act(async () => {
      await result.current.changeRoomSettings({
        gameType: 'CS',
        maxPlayers: 2,
        category: 'DEFAULT',
        totalRound: 5,
        difficulty: 0,
        csDifficulty: 'HARD',
      });
    });

    expect(window.alert).toHaveBeenCalledWith('방이 가득찼습니다.');
    expect(result.current.roomMaxPlayers).toBe(before);
  });
});
