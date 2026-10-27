package com.fungame.songquiz.support.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class LogContextTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("연 동안 넣은 값이 보이고, 닫으면 지워진다.")
    void values_live_until_closed() {
        try (LogContext context = LogContext.open().put(LogContext.ROOM_ID, 42L)) {
            assertThat(MDC.get(LogContext.ROOM_ID)).isEqualTo("42");
        }

        assertThat(MDC.get(LogContext.ROOM_ID)).isNull();
    }

    @Test
    @DisplayName("닫으면 덮어쓰기 전의 값으로 돌아간다. 바깥 문맥을 지우지 않는다.")
    void closing_restores_the_outer_value() {
        MDC.put(LogContext.REQUEST_ID, "outer");

        try (LogContext context = LogContext.open()
                .put(LogContext.REQUEST_ID, "inner")
                .put(LogContext.REQUEST_ID, "inner-again")) {
            assertThat(MDC.get(LogContext.REQUEST_ID)).isEqualTo("inner-again");
        }

        assertThat(MDC.get(LogContext.REQUEST_ID)).isEqualTo("outer");
    }

    @Test
    @DisplayName("값이 없으면 넣지 않는다. 로그에 null 이 찍히지 않는다.")
    void a_missing_value_is_left_out() {
        try (LogContext context = LogContext.open().put(LogContext.MEMBER_ID, null)) {
            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        }
    }
}
