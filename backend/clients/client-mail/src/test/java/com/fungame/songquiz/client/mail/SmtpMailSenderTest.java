package com.fungame.songquiz.client.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpMailSenderTest {

    private static final String FROM = "no-reply@fun-game.club";
    private static final String TO = "receiver@fun-game.club";
    private static final String SUBJECT = "[FunGame] 비밀번호 재설정 안내";
    private static final String BODY = "본문";

    private JavaMailSender javaMailSender;
    private SmtpMailSender smtpMailSender;

    @BeforeEach
    void setUp() {
        javaMailSender = mock(JavaMailSender.class);
        smtpMailSender = new SmtpMailSender(javaMailSender, FROM);
    }

    @Test
    @DisplayName("발신 주소, 수신자, 제목, 본문을 담아 SMTP 로 보낸다.")
    void sendsMessage() {
        smtpMailSender.send(TO, SUBJECT, BODY);

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(message.capture());

        assertThat(message.getValue().getFrom()).isEqualTo(FROM);
        assertThat(message.getValue().getTo()).containsExactly(TO);
        assertThat(message.getValue().getSubject()).isEqualTo(SUBJECT);
        assertThat(message.getValue().getText()).isEqualTo(BODY);
    }

    @Test
    @DisplayName("발송에 실패해도 예외를 호출한 쪽으로 넘기지 않는다.")
    void swallowsSendFailure() {
        doThrow(new MailSendException("SMTP 서버가 응답하지 않습니다"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> smtpMailSender.send(TO, SUBJECT, BODY))
                .doesNotThrowAnyException();
    }
}
