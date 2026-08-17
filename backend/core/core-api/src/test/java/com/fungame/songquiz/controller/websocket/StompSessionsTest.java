package com.fungame.songquiz.controller.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StompSessionsTest {

    private static final Long MEMBER_ID = 11L;
    private static final Long OTHER_MEMBER_ID = 22L;

    private final StompSessions stompSessions = new StompSessions();

    @Test
    @DisplayName("세션이 없는 회원은 접속 중이 아니다.")
    void memberWithoutSessionIsNotConnected() {
        assertThat(stompSessions.isConnected(MEMBER_ID)).isFalse();
        assertThat(stompSessions.countSessionsOf(MEMBER_ID)).isZero();
    }

    @Test
    @DisplayName("한 회원이 탭을 여러 개 열면 세션도 그만큼 센다.")
    void countEverySessionOfMember() {
        stompSessions.add("session-1", MEMBER_ID);
        stompSessions.add("session-2", MEMBER_ID);
        stompSessions.add("session-3", OTHER_MEMBER_ID);

        assertThat(stompSessions.countSessionsOf(MEMBER_ID)).isEqualTo(2);
        assertThat(stompSessions.connectedMemberIds()).containsExactlyInAnyOrder(MEMBER_ID, OTHER_MEMBER_ID);
    }

    @Test
    @DisplayName("세션을 지우면 누구의 세션이었는지 알려준다.")
    void removeTellsWhoseSessionItWas() {
        stompSessions.add("session-1", MEMBER_ID);

        assertThat(stompSessions.remove("session-1")).isEqualTo(MEMBER_ID);
        assertThat(stompSessions.isConnected(MEMBER_ID)).isFalse();
    }

    @Test
    @DisplayName("등록된 적 없는 세션을 지우면 아무도 알려주지 않는다.")
    void removeUnknownSessionTellsNobody() {
        assertThat(stompSessions.remove("등록된적-없는-세션")).isNull();
    }

    @Test
    @DisplayName("탭 하나가 닫혀도 남은 탭이 있으면 접속 중이다.")
    void stillConnectedWhileAnotherTabIsOpen() {
        stompSessions.add("session-1", MEMBER_ID);
        stompSessions.add("session-2", MEMBER_ID);

        stompSessions.remove("session-1");

        assertThat(stompSessions.isConnected(MEMBER_ID)).isTrue();
    }
}
