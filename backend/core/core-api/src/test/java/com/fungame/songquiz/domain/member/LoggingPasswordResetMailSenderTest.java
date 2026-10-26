package com.fungame.songquiz.domain.member;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LoggingPasswordResetMailSenderTest {

    private static final String EMAIL = "someone@fun-game.club";
    private static final String RESET_LINK = "http://localhost:5173/reset?token=raw-token";

    private final LoggingPasswordResetMailSender sender = new LoggingPasswordResetMailSender();
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void listenToLogs() {
        logs.start();
        senderLogger().addAppender(logs);
    }

    @AfterEach
    void stopListening() {
        senderLogger().detachAppender(logs);
        logs.stop();
    }

    @Test
    @DisplayName("로컬에서 쓸 재설정 링크는 남기고, 받는 사람의 주소는 남기지 않는다.")
    void logs_the_link_without_the_recipient() {
        sender.send(EMAIL, RESET_LINK);

        assertThat(logs.list).singleElement()
                .extracting(ILoggingEvent::getFormattedMessage).asString()
                .contains(RESET_LINK)
                .doesNotContain(EMAIL);
    }

    private static Logger senderLogger() {
        return (Logger) LoggerFactory.getLogger(LoggingPasswordResetMailSender.class);
    }
}
