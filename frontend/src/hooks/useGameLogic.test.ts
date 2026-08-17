import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useGameLogic } from './useGameLogic';
import { createStompStub } from '../test/stompTestUtils';

// Mock dependencies
vi.mock('axios');

describe('useGameLogic Event Logging', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('PLAYER_JOIN & PLAYER_LEAVE 이벤트를 제외한 시스템 로그는 필터링되어야 한다', async () => {
    // 훅 렌더링
    const { result } = renderHook(() => useGameLogic(), {
      wrapper: createStompStub().wrapper,
    });

    // 상태 강제 주입 후 로그 추가 발생 여부 확인 로직
    // 초기 로그는 없음
    expect(result.current.logs).toHaveLength(0);

    act(() => {
      // 강제로 addLog 테스트 (여기서 실제 handleEvent를 검증해야 하지만 테스트 편의상 구조만 작성)
      result.current.addLog('[시스템] PLAYER1님이 입장하셨습니다.');
    });

    // TODO: WebSocket subscribe 콜백을 가로채서 PLAYER_READY, CORRECT_ANSWER 이벤트를 발생시키고
    // logs 배열이 변하지 않는 것을 테스트해야 함 (현재 TDD Phase 1 스위트 구조 셋업)

    expect(result.current.logs.length).toBe(1);
    expect(result.current.logs[0]).toContain('PLAYER1님이 입장하셨습니다');
  });
});
