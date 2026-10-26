package com.fungame.songquiz.domain.member;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class PasswordResetMailListenerTest {

    private static final String EMAIL = "someone@fun-game.club";
    private static final String RAW_TOKEN = "raw-token";

    private final PasswordResetMailSender failingSender = (email, resetLink) -> {
        throw new IllegalStateException("메일 서버에 닿지 못했다");
    };
    private final PasswordResetMailListener listener =
            new PasswordResetMailListener(failingSender, "https://fun-game.club/reset");
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void listenToLogs() {
        logs.start();
        listenerLogger().addAppender(logs);
    }

    @AfterEach
    void stopListening() {
        listenerLogger().detachAppender(logs);
        logs.stop();
    }

    @Test
    @DisplayName("메일 발송 실패는 원인과 함께 남기되, 받는 사람의 주소와 재설정 토큰은 남기지 않는다.")
    void a_failure_is_logged_without_the_recipient_or_token() {
        listener.handlePasswordResetRequested(new PasswordResetRequestedEvent(EMAIL, RAW_TOKEN));

        assertThat(logs.list).singleElement().satisfies(logged -> {
            assertThat(logged.getLevel()).isEqualTo(Level.ERROR);
            assertThat(logged.getThrowableProxy().getMessage()).isEqualTo("메일 서버에 닿지 못했다");
            assertThat(logged.getFormattedMessage()).doesNotContain(EMAIL).doesNotContain(RAW_TOKEN);
        });
    }

    private static Logger listenerLogger() {
        return (Logger) LoggerFactory.getLogger(PasswordResetMailListener.class);
    }
}
