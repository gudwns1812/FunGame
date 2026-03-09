import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import WaitingRoom from './WaitingRoom';

describe('WaitingRoom 키보드 인터랙션', () => {
    const mockProps = {
        players: [],
        onStart: vi.fn(),
        onLeave: vi.fn(),
        onToggleReady: vi.fn(),
        isHost: false,
        logs: [],
        onSendMessage: vi.fn()
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
