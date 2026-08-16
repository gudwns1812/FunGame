package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.session.GameResultEvent;
import com.fungame.songquiz.domain.session.GameStartEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MemberPresenceEventListenerTest {

    private final MemberPresenceService memberPresenceService = mock(MemberPresenceService.class);
    private final MemberPresenceEventListener listener = new MemberPresenceEventListener(memberPresenceService);

    @Test
    @DisplayName("게임이 시작되면 그 방의 회원을 게임중으로 표시한다.")
    void markPlayingOnGameStart() {
        listener.handleGameStart(new GameStartEvent(7L, null));

        verify(memberPresenceService).markRoomPlaying(7L);
    }

    @Test
    @DisplayName("게임이 끝나면 그 방의 회원을 대기 상태로 되돌린다.")
    void markWaitingOnGameResult() {
        listener.handleGameResult(new GameResultEvent(7L, List.of()));

        verify(memberPresenceService).markRoomWaiting(7L);
    }
}
