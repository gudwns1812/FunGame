import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { useOnlineMembers } from './useOnlineMembers';
import { createStompStub } from '../test/stompTestUtils';
import { PRESENCE_QUEUE } from '../utils/stompDestination';
import type { OnlineMember } from '../types/presence';

vi.mock('axios');

const mockedAxios = axios as unknown as { get: ReturnType<typeof vi.fn> };

const onlineMemberFetchCount = () =>
  mockedAxios.get.mock.calls.filter((call) => call[0] === '/api/members/online').length;

const OTHER_MEMBER: OnlineMember = {
  memberId: 2,
  nickname: '다른 사람',
  status: 'LOBBY',
  currentRoomId: null,
};

describe('useOnlineMembers 접속자 목록 동기화', () => {
  let stomp: ReturnType<typeof createStompStub>;

  const renderEnabled = () => {
    stomp = createStompStub();
    return renderHook(() => useOnlineMembers(true), { wrapper: stomp.wrapper });
  };

  const connect = () =>
    act(async () => {
      await stomp.connect();
    });

  beforeEach(() => {
    vi.clearAllMocks();
    mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: [] } });
  });

  it('연결이 맺어지면 접속자 알림을 구독하고 목록을 한 번 가져온다', async () => {
    renderEnabled();

    await connect();

    expect(stomp.subscriberCountOf(PRESENCE_QUEUE)).toBe(1);
    expect(onlineMemberFetchCount()).toBe(1);
  });

  it('접속 변경 알림에 실린 목록으로 갱신한다', async () => {
    const { result } = renderEnabled();
    await connect();

    act(() => stomp.emit(PRESENCE_QUEUE, [OTHER_MEMBER]));

    expect(result.current).toEqual([OTHER_MEMBER]);
  });

  it('알림에 목록이 실려 오면 다시 가져오지 않는다', async () => {
    renderEnabled();
    await connect();
    const fetchesBefore = onlineMemberFetchCount();

    act(() => stomp.emit(PRESENCE_QUEUE, [OTHER_MEMBER]));

    expect(onlineMemberFetchCount()).toBe(fetchesBefore);
  });

  it('해석할 수 없는 알림을 받으면 목록을 다시 가져와 보정한다', async () => {
    renderEnabled();
    await connect();
    const fetchesBefore = onlineMemberFetchCount();

    await act(async () => {
      stomp.emit(PRESENCE_QUEUE, 'REFRESH');
    });

    expect(onlineMemberFetchCount()).toBe(fetchesBefore + 1);
  });

  it('끊겼다 다시 연결되면 구독을 다시 걸고 목록을 다시 가져온다', async () => {
    renderEnabled();
    await connect();
    const fetchesBefore = onlineMemberFetchCount();

    await connect();

    expect(stomp.subscriberCountOf(PRESENCE_QUEUE)).toBe(1);
    expect(onlineMemberFetchCount()).toBe(fetchesBefore + 1);
  });

  it('꺼지면 구독을 정리한다', async () => {
    const stub = createStompStub();
    const { unmount } = renderHook(() => useOnlineMembers(true), { wrapper: stub.wrapper });
    await act(async () => {
      await stub.connect();
    });
    await waitFor(() => expect(stub.subscriberCountOf(PRESENCE_QUEUE)).toBe(1));

    unmount();

    expect(stub.subscriberCountOf(PRESENCE_QUEUE)).toBe(0);
  });
});
