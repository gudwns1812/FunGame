import { render, screen, waitFor, act, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import axios from 'axios';
import OnlineUserList from './OnlineUserList';
import { createSseStub } from '../test/sseTestUtils';
import type { OnlineMember } from '../types/presence';

vi.mock('axios');

const mockedAxios = axios as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
};

const members: OnlineMember[] = [
  { memberId: 1, nickname: '로비유저', status: 'LOBBY', currentRoomId: null },
  { memberId: 2, nickname: '대기유저', status: 'WAITING', currentRoomId: 9 },
  { memberId: 3, nickname: '게임유저', status: 'PLAYING', currentRoomId: 9 },
];

const renderList = (invitingRoomId?: string | null) => {
  const sse = createSseStub();
  const { wrapper: Wrapper } = sse;
  render(
    <Wrapper>
      <OnlineUserList invitingRoomId={invitingRoomId} />
    </Wrapper>,
  );
  return sse;
};

const inviteButtonOf = (nickname: string) => {
  const row = screen.getByText(nickname).closest('div')!.parentElement!;
  return row.querySelector('button')!;
};

describe('OnlineUserList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: members } });
    mockedAxios.post = vi.fn().mockResolvedValue({
      data: { result: 'SUCCESS', data: { expiresInSeconds: 30 } },
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('접속 중인 유저와 각자의 위치를 보여준다', async () => {
    renderList();

    expect(await screen.findByText('로비유저')).toBeInTheDocument();
    expect(screen.getByText('로비')).toBeInTheDocument();
    expect(screen.getByText('대기실')).toBeInTheDocument();
    expect(screen.getByText('게임중')).toBeInTheDocument();
  });

  it('로비에서는 초대 버튼을 보여주지 않는다', async () => {
    renderList();

    await screen.findByText('로비유저');
    expect(screen.queryByRole('button', { name: '초대' })).not.toBeInTheDocument();
  });

  it('대기실에서는 로비에 있는 유저만 초대할 수 있다', async () => {
    renderList('7');

    await screen.findByText('로비유저');
    expect(inviteButtonOf('로비유저')).toBeEnabled();
    expect(inviteButtonOf('대기유저')).toBeDisabled();
    expect(inviteButtonOf('게임유저')).toBeDisabled();
  });

  it('초대를 보내면 그 방으로 초대 요청을 한다', async () => {
    const user = userEvent.setup();
    renderList('7');
    await screen.findByText('로비유저');

    await user.click(inviteButtonOf('로비유저'));

    expect(mockedAxios.post).toHaveBeenCalledWith('/api/rooms/7/invites', { targetMemberId: 1 });
  });

  it('초대를 보낸 뒤에는 같은 사람에게 다시 보낼 수 없다', async () => {
    const user = userEvent.setup();
    renderList('7');
    await screen.findByText('로비유저');

    await user.click(inviteButtonOf('로비유저'));

    await waitFor(() => expect(inviteButtonOf('로비유저')).toBeDisabled());
    expect(inviteButtonOf('로비유저')).toHaveTextContent('보냄');
  });

  it('초대가 만료되면 다시 보낼 수 있게 되돌아온다', async () => {
    renderList('7');
    await screen.findByText('로비유저');
    vi.useFakeTimers();

    fireEvent.click(inviteButtonOf('로비유저'));
    await act(async () => {});
    expect(inviteButtonOf('로비유저')).toHaveTextContent('보냄');

    act(() => {
      vi.advanceTimersByTime(30000);
    });

    expect(inviteButtonOf('로비유저')).toHaveTextContent('초대');
    expect(inviteButtonOf('로비유저')).toBeEnabled();
  });

  it('만료 전에는 계속 보냄 상태를 유지한다', async () => {
    renderList('7');
    await screen.findByText('로비유저');
    vi.useFakeTimers();

    fireEvent.click(inviteButtonOf('로비유저'));
    await act(async () => {});

    act(() => {
      vi.advanceTimersByTime(29000);
    });

    expect(inviteButtonOf('로비유저')).toHaveTextContent('보냄');
  });

  it('초대에 실패하면 사유를 알리고 다시 보낼 수 있게 되돌린다', async () => {
    const user = userEvent.setup();
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    mockedAxios.post = vi.fn().mockRejectedValue({
      response: { data: { error: { message: '로비에 있는 사용자만 초대할 수 있습니다.' } } },
    });
    renderList('7');
    await screen.findByText('로비유저');

    await user.click(inviteButtonOf('로비유저'));

    await waitFor(() => expect(alertSpy).toHaveBeenCalledWith('로비에 있는 사용자만 초대할 수 있습니다.'));
    expect(inviteButtonOf('로비유저')).toBeEnabled();
  });

  it('접속 상태가 바뀌면 목록을 다시 가져온다', async () => {
    const sse = renderList();
    await screen.findByText('로비유저');
    const fetchesBefore = mockedAxios.get.mock.calls.length;

    sse.emit('presence-update', 'REFRESH');

    await waitFor(() => expect(mockedAxios.get.mock.calls.length).toBe(fetchesBefore + 1));
  });

  it('아무도 없으면 안내 문구를 보여준다', async () => {
    mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: [] } });

    renderList();

    expect(await screen.findByText('아무도 접속해 있지 않습니다')).toBeInTheDocument();
  });
});
