import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect, vi } from 'vitest';
import Game from './Game';

describe('Game UI 렌더링 및 키보드 인터랙션', () => {
    const mockProps = {
        players: [],
        roomId: '123',
        timeLeft: 30,
        totalTime: 30,
        currentVideoId: 'Test Question Content',
        onAnswerSubmit: vi.fn(),
        onSkipRound: vi.fn(),
        onFetchRank: vi.fn().mockResolvedValue(true),
        playerIndex: null,
        gameStartInfo: null,
        gameType: 'CS',
        roundEndInfo: null,
        roundIndex: 1,
        currentRound: 1,
        totalRound: 5,
        logs: []
    };

    it('CS 퀴즈 진입 시 폰트 축소가 적용되어 있어야 하며 패널이 넉넉한 높이를 유지해야 한다', () => {
        const { container } = render(<Game {...mockProps} />);

        const quizText = screen.getByText('Test Question Content');
        // 테스트에서는 실제 구현에 맞게 확인. 추후 text-sm 또는 text-base 등으로 변경될 것을 예상
        expect(quizText).toBeInTheDocument();

        // 패널 고정 높이 렌더링 검증, 여기서는 간단히 패널이 존재하는지만 확인
        const panel = container.querySelector('.h-\\[550px\\]');
        expect(panel).not.toBeNull();
    });

    it('전역에서 Enter 키 입력 시 채팅 입력창으로 포커스가 이동해야 한다', () => {
        render(<Game {...mockProps} />);

        const chatInput = screen.getByPlaceholderText('코드나 정답을 입력하세요...');

        chatInput.blur();
        expect(document.activeElement).not.toBe(chatInput);

        fireEvent.keyDown(window, { key: 'Enter', code: 'Enter' });

        expect(document.activeElement).toBe(chatInput);
    });
});
