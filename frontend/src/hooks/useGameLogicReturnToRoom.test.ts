import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { useGameLogic } from './useGameLogic';

vi.mock('axios');
vi.mock('sockjs-client');

const mockedAxios = axios as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  defaults: Partial<typeof axios.defaults>;
};

describe('useGameLogic 결과창에서 게임방으로 돌아가기', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 0 } });
    mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: [] } });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
  });

  it('지난 판의 로그를 지운 채로 대기실에 들어간다', async () => {
    const { result } = renderHook(() => useGameLogic());

    act(() => {
      result.current.addLog('[시스템] 1라운드 정답은 밤양갱');
      result.current.addLog('참가자: 정답!');
    });
    expect(result.current.logs).toHaveLength(2);

    await act(async () => {
      await result.current.returnToWaitingRoom();
    });

    expect(result.current.logs).toHaveLength(0);
    expect(result.current.status).toBe('WAITING');
  });

  it('진행 중이던 라운드 화면 상태도 함께 비운다', async () => {
    const { result } = renderHook(() => useGameLogic());

    await act(async () => {
      await result.current.returnToWaitingRoom();
    });

    expect(result.current.hint).toBe('');
    expect(result.current.currentVideoId).toBe('');
    expect(localStorage.getItem('ums_currentVideoId')).toBeNull();
  });
});
