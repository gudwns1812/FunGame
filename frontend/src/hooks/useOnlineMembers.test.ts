import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { useOnlineMembers } from './useOnlineMembers';
import { createSseStub } from '../test/sseTestUtils';
import type { OnlineMember } from '../types/presence';

vi.mock('axios');

const mockedAxios = axios as unknown as { get: ReturnType<typeof vi.fn> };

const onlineMemberFetchCount = () =>
  mockedAxios.get.mock.calls.filter((call) => call[0] === '/api/members/online').length;

const DEBOUNCE_SETTLE_MS = 400;

const letDebounceSettle = () =>
  act(async () => {
    await new Promise((resolve) => setTimeout(resolve, DEBOUNCE_SETTLE_MS));
  });

const OTHER_MEMBER: OnlineMember = {
  memberId: 2,
  nickname: '다른 사람',
  status: 'LOBBY',
  currentRoomId: null,
};

describe('useOnlineMembers 접속자 목록 동기화', () => {
  let sse: ReturnType<typeof createSseStub>;

  const renderEnabled = () =>
    renderHook(() => useOnlineMembers(true), { wrapper: sse.wrapper });

  beforeEach(() => {
    vi.clearAllMocks();
    mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: [] } });
    sse = createSseStub();
  });

  it('처음 켜지면 접속자 목록을 한 번 가져온다', async () => {
    renderEnabled();

    await letDebounceSettle();

    expect(onlineMemberFetchCount()).toBe(1);
  });

  it('접속 변경 알림에 실린 목록으로 갱신한다', async () => {
    const { result } = renderEnabled();
    await letDebounceSettle();

    act(() => sse.emit('presence-update', JSON.stringify([OTHER_MEMBER])));
    await letDebounceSettle();

    expect(result.current).toEqual([OTHER_MEMBER]);
  });

  it('알림에 목록이 실려 오면 다시 가져오지 않는다', async () => {
    renderEnabled();
    await letDebounceSettle();
    const fetchesBefore = onlineMemberFetchCount();

    act(() => sse.emit('presence-update', JSON.stringify([OTHER_MEMBER])));
    await letDebounceSettle();

    expect(onlineMemberFetchCount()).toBe(fetchesBefore);
  });

  it('해석할 수 없는 알림을 받으면 목록을 다시 가져와 보정한다', async () => {
    renderEnabled();
    await letDebounceSettle();
    const fetchesBefore = onlineMemberFetchCount();

    act(() => sse.emit('presence-update', 'REFRESH'));
    await letDebounceSettle();

    expect(onlineMemberFetchCount()).toBe(fetchesBefore + 1);
  });

  it('끊겼다 다시 연결되면 목록을 다시 가져온다', async () => {
    renderEnabled();
    await letDebounceSettle();
    const fetchesBefore = onlineMemberFetchCount();

    act(() => sse.emit('connected', 'Connected'));
    await letDebounceSettle();

    expect(onlineMemberFetchCount()).toBe(fetchesBefore + 1);
  });

  it('꺼지면 구독을 정리한다', async () => {
    const { unmount } = renderEnabled();
    await waitFor(() => expect(sse.listenerCountOf('presence-update')).toBe(1));

    unmount();

    expect(sse.listenerCountOf('presence-update')).toBe(0);
  });
});
