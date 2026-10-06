package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StompSessionRegistryTest {

    private static final Long MEMBER_ID = 11L;
    private static final Long OTHER_MEMBER_ID = 22L;

    private final StompSessionRegistry stompSessionRegistry = new StompSessionRegistry();

    @Test
    @DisplayName("이 서버에 세션이 없는 회원은 센 세션이 없다.")
    void memberWithoutSessionHasNone() {
        assertThat(stompSessionRegistry.countSessionsOf(MEMBER_ID)).isZero();
    }

    @Test
    @DisplayName("한 회원이 탭을 여러 개 열면 세션도 그만큼 센다.")
    void countEverySessionOfMember() {
        stompSessionRegistry.add("session-1", MEMBER_ID);
        stompSessionRegistry.add("session-2", MEMBER_ID);
        stompSessionRegistry.add("session-3", OTHER_MEMBER_ID);

        assertThat(stompSessionRegistry.countSessionsOf(MEMBER_ID)).isEqualTo(2);
        assertThat(stompSessionRegistry.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("세션을 지우면 누구의 세션이었는지 알려준다.")
    void removeTellsWhoseSessionItWas() {
        stompSessionRegistry.add("session-1", MEMBER_ID);

        assertThat(stompSessionRegistry.remove("session-1")).isEqualTo(MEMBER_ID);
        assertThat(stompSessionRegistry.countSessionsOf(MEMBER_ID)).isZero();
    }

    @Test
    @DisplayName("등록된 적 없는 세션을 지우면 아무도 알려주지 않는다.")
    void removeUnknownSessionTellsNobody() {
        assertThat(stompSessionRegistry.remove("등록된적-없는-세션")).isNull();
    }

    @Test
    @DisplayName("탭 하나가 닫혀도 남은 탭의 세션은 남는다.")
    void remainingTabKeepsItsSession() {
        stompSessionRegistry.add("session-1", MEMBER_ID);
        stompSessionRegistry.add("session-2", MEMBER_ID);

        stompSessionRegistry.remove("session-1");

        assertThat(stompSessionRegistry.countSessionsOf(MEMBER_ID)).isEqualTo(1);
    }
}
