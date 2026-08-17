import { render, screen, waitFor, act, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import axios from 'axios';
import OnlineUserList from './OnlineUserList';
import ToastStack from './ToastStack';
import { createStompStub } from '../test/stompTestUtils';
import { PRESENCE_QUEUE } from '../utils/stompDestination';
import { clearToasts } from '../utils/toast';
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
  const stomp = createStompStub({ startConnected: true });
  const { wrapper: Wrapper } = stomp;
  render(
    <Wrapper>
      <OnlineUserList invitingRoomId={invitingRoomId} />
      <ToastStack />
    </Wrapper>,
  );
  return stomp;
};

const searchToggle = () => screen.getByRole('button', { name: '닉네임 검색' });

const searchBox = () => screen.getByRole('textbox', { name: '닉네임으로 접속자 검색' });

const inviteButtonOf = (nickname: string) => {
  const row = screen.getByText(nickname).closest('div')!.parentElement!;
  return row.querySelector('button')!;
};

const groupHeaderOf = (label: string) => screen.getByRole('button', { name: new RegExp(label) });

const expandGroup = (label: string) => fireEvent.click(groupHeaderOf(label));

const memberCountIn = (label: string) => groupHeaderOf(label).querySelector('.px-chip')!.textContent;

describe('OnlineUserList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearToasts();
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

  it('로비 유저와 그 외 유저를 각자의 그룹 아래에 나눠 보여준다', async () => {
    renderList();
    await screen.findByText('로비유저');

    const lobbyGroup = groupHeaderOf('로비에 있음').closest('section')!;
    const elsewhereGroup = groupHeaderOf('다른 방에 있음').closest('section')!;

    expect(lobbyGroup).toHaveTextContent('로비유저');
    expect(lobbyGroup).not.toHaveTextContent('대기유저');
    expect(elsewhereGroup).toHaveTextContent('대기유저');
    expect(elsewhereGroup).toHaveTextContent('게임유저');
    expect(elsewhereGroup).not.toHaveTextContent('로비유저');
  });

  it('그룹 헤더를 누르면 접히고 다시 누르면 펼쳐진다', async () => {
    renderList();
    await screen.findByText('로비유저');

    expandGroup('로비에 있음');
    expect(screen.queryByText('로비유저')).not.toBeInTheDocument();

    expandGroup('로비에 있음');
    expect(screen.getByText('로비유저')).toBeInTheDocument();
  });

  it('접힌 그룹도 헤더의 인원 수는 그대로 보인다', async () => {
    renderList();
    await screen.findByText('로비유저');

    expandGroup('다른 방에 있음');

    expect(screen.queryByText('대기유저')).not.toBeInTheDocument();
    expect(memberCountIn('다른 방에 있음')).toBe('2');
  });

  it('대기실에서는 로비 그룹만 펼쳐진 채로 시작한다', async () => {
    renderList('7');
    await screen.findByText('로비유저');

    expect(groupHeaderOf('로비에 있음')).toHaveAttribute('aria-expanded', 'true');
    expect(groupHeaderOf('다른 방에 있음')).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByText('대기유저')).not.toBeInTheDocument();
  });

  it('로비에서는 두 그룹 모두 펼쳐진 채로 시작한다', async () => {
    renderList();
    await screen.findByText('로비유저');

    expect(groupHeaderOf('로비에 있음')).toHaveAttribute('aria-expanded', 'true');
    expect(groupHeaderOf('다른 방에 있음')).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText('대기유저')).toBeInTheDocument();
  });

  it('비어 있는 그룹은 0 과 안내 문구를 보여준다', async () => {
    mockedAxios.get = vi.fn().mockResolvedValue({
      data: { result: 'SUCCESS', data: [members[0]] },
    });

    renderList();
    await screen.findByText('로비유저');

    expect(memberCountIn('다른 방에 있음')).toBe('0');
    expect(screen.getByText('아무도 없습니다')).toBeInTheDocument();
  });

  it('로비에서는 초대 버튼을 보여주지 않는다', async () => {
    renderList();

    await screen.findByText('로비유저');
    expect(screen.queryByRole('button', { name: '초대' })).not.toBeInTheDocument();
  });

  it('대기실에서는 로비에 있는 유저만 초대할 수 있다', async () => {
    renderList('7');

    await screen.findByText('로비유저');
    expandGroup('다른 방에 있음');

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
    mockedAxios.post = vi.fn().mockRejectedValue({
      response: { data: { error: { message: '로비에 있는 사용자만 초대할 수 있습니다.' } } },
    });
    renderList('7');
    await screen.findByText('로비유저');

    await user.click(inviteButtonOf('로비유저'));

    expect(await screen.findByRole('alert')).toHaveTextContent('로비에 있는 사용자만 초대할 수 있습니다.');
    expect(inviteButtonOf('로비유저')).toBeEnabled();
  });

  it('해석할 수 없는 접속 알림을 받으면 목록을 다시 가져온다', async () => {
    const stomp = renderList();
    await screen.findByText('로비유저');
    const fetchesBefore = mockedAxios.get.mock.calls.length;

    act(() => stomp.emit(PRESENCE_QUEUE, 'REFRESH'));

    await waitFor(() => expect(mockedAxios.get.mock.calls.length).toBe(fetchesBefore + 1));
  });

  it('접속 알림에 실려 온 목록으로 갱신한다', async () => {
    const stomp = renderList();
    await screen.findByText('로비유저');

    act(() => stomp.emit(PRESENCE_QUEUE, [members[0]]));

    await waitFor(() => expect(screen.queryByText('대기유저')).not.toBeInTheDocument());
    expect(screen.getByText('로비유저')).toBeInTheDocument();
  });

  it('돋보기를 누르기 전에는 검색창이 없다', async () => {
    renderList();
    await screen.findByText('로비유저');

    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    expect(screen.getByText('접속 중')).toBeInTheDocument();
  });

  it('돋보기를 누르면 검색창이 나오고 바로 입력할 수 있다', async () => {
    const user = userEvent.setup();
    renderList();
    await screen.findByText('로비유저');

    await user.click(searchToggle());

    expect(searchBox()).toHaveFocus();
  });

  it('입력한 닉네임을 가진 유저만 남긴다', async () => {
    const user = userEvent.setup();
    renderList();
    await screen.findByText('로비유저');

    await user.click(searchToggle());
    await user.type(searchBox(), '대기');

    expect(screen.getByText('대기유저')).toBeInTheDocument();
    expect(screen.queryByText('로비유저')).not.toBeInTheDocument();
    expect(screen.queryByText('게임유저')).not.toBeInTheDocument();
  });

  it('검색 중에는 접혀 있던 그룹도 결과를 보여준다', async () => {
    const user = userEvent.setup();
    renderList('7');
    await screen.findByText('로비유저');
    expect(screen.queryByText('대기유저')).not.toBeInTheDocument();

    await user.click(searchToggle());
    await user.type(searchBox(), '대기');

    expect(screen.getByText('대기유저')).toBeInTheDocument();
  });

  it('검색 결과가 없으면 안내 문구를 보여준다', async () => {
    const user = userEvent.setup();
    renderList();
    await screen.findByText('로비유저');

    await user.click(searchToggle());
    await user.type(searchBox(), '없는사람');

    expect(screen.getByText('검색 결과가 없습니다')).toBeInTheDocument();
  });

  it('전체 접속자 수는 검색 중에도 그대로 보여준다', async () => {
    const user = userEvent.setup();
    renderList();
    await screen.findByText('로비유저');

    await user.click(searchToggle());
    await user.type(searchBox(), '대기');

    expect(searchToggle().nextElementSibling).toHaveTextContent('3');
  });

  it('돋보기를 다시 누르면 검색창이 닫히고 필터가 풀린다', async () => {
    const user = userEvent.setup();
    renderList();
    await screen.findByText('로비유저');

    await user.click(searchToggle());
    await user.type(searchBox(), '대기');
    await user.click(searchToggle());

    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    expect(screen.getByText('로비유저')).toBeInTheDocument();
  });

  it('검색창에서 ESC 를 누르면 검색창이 닫히고 필터가 풀린다', async () => {
    const user = userEvent.setup();
    renderList();
    await screen.findByText('로비유저');

    await user.click(searchToggle());
    await user.type(searchBox(), '대기{Escape}');

    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    expect(screen.getByText('로비유저')).toBeInTheDocument();
  });

  it('아무도 없으면 안내 문구를 보여준다', async () => {
    mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: [] } });

    renderList();

    expect(await screen.findByText('아무도 접속해 있지 않습니다')).toBeInTheDocument();
  });
});
