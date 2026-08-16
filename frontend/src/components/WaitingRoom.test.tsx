import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import WaitingRoom from './WaitingRoom';

describe('WaitingRoom 키보드 인터랙션', () => {
    const mockProps = {
        players: [],
        onStart: vi.fn(),
        onLeave: vi.fn(),
        onToggleReady: vi.fn(),
        onKickPlayer: vi.fn(),
        isHost: false,
        maxPlayers: 12,
        logs: [],
        onSendMessage: vi.fn(),
        roomSettings: null,
        myMemberId: 1,
        onChangeSettings: vi.fn()
    };

    it('전역에서 Enter 키 입력 시 채팅 입력창으로 포커스가 이동해야 한다', () => {
        render(<WaitingRoom {...mockProps} />);

        const chatInput = screen.getByPlaceholderText('메시지 입력...');

        // 처음에 포커스가 없는 상태라고 가정 (컴포넌트 내에 autoFocus가 있으므로, blur 시킴)
        chatInput.blur();
        expect(document.activeElement).not.toBe(chatInput);

        // 전역 Enter 키 발생
        fireEvent.keyDown(window, { key: 'Enter', code: 'Enter' });

        // 포커스가 이동했는지 확인
        expect(document.activeElement).toBe(chatInput);
    });
});

describe('WaitingRoom 강퇴', () => {
    const HOST_MEMBER_ID = 1;
    const GUEST_MEMBER_ID = 2;

    const players = [
        { memberId: HOST_MEMBER_ID, name: '방장', isHost: true, isReady: true, score: 0, colorIndex: 0 },
        { memberId: GUEST_MEMBER_ID, name: '참가자', isHost: false, isReady: false, score: 0, colorIndex: 1 },
    ];

    const renderWaitingRoom = (overrides: Partial<React.ComponentProps<typeof WaitingRoom>> = {}) => {
        const onKickPlayer = vi.fn();
        render(
            <WaitingRoom
                players={players}
                onStart={vi.fn()}
                onLeave={vi.fn()}
                onToggleReady={vi.fn()}
                onKickPlayer={onKickPlayer}
                isHost
                maxPlayers={8}
                logs={[]}
                onSendMessage={vi.fn()}
                roomSettings={null}
                myMemberId={HOST_MEMBER_ID}
                onChangeSettings={vi.fn()}
                {...overrides}
            />,
        );
        return { onKickPlayer };
    };

    it('방장에게는 다른 참가자마다 내보내기 버튼이 보인다', () => {
        renderWaitingRoom();

        expect(screen.getAllByRole('button', { name: '참가자 내보내기' })).toHaveLength(1);
    });

    it('방장이 아니면 내보내기 버튼이 보이지 않는다', () => {
        renderWaitingRoom({ isHost: false, myMemberId: GUEST_MEMBER_ID });

        expect(screen.queryByRole('button', { name: /내보내기$/ })).toBeNull();
    });

    it('확인 창에서 내보내기를 누르면 그 참가자를 강퇴한다', () => {
        const { onKickPlayer } = renderWaitingRoom();

        fireEvent.click(screen.getByRole('button', { name: '참가자 내보내기' }));
        fireEvent.click(screen.getByRole('button', { name: '내보내기' }));

        expect(onKickPlayer).toHaveBeenCalledWith(GUEST_MEMBER_ID);
    });

    it('확인 창에서 취소하면 아무도 강퇴되지 않는다', () => {
        const { onKickPlayer } = renderWaitingRoom();

        fireEvent.click(screen.getByRole('button', { name: '참가자 내보내기' }));
        fireEvent.click(screen.getByRole('button', { name: '취소' }));

        expect(onKickPlayer).not.toHaveBeenCalled();
        expect(screen.queryByRole('button', { name: '내보내기' })).toBeNull();
    });
});
