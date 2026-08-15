package com.fungame.songquiz.support.mail;

import com.fungame.songquiz.client.mail.SesMailSender;
import com.fungame.songquiz.domain.member.PasswordResetTokenGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SesPasswordResetMailSenderTest {

    private static final String EMAIL = "receiver@fun-game.club";
    private static final String RESET_LINK = "https://www.fun-game.club/reset-password?token=abc";

    @Mock
    private SesMailSender sesMailSender;

    @InjectMocks
    private SesPasswordResetMailSender passwordResetMailSender;

    @Test
    @DisplayName("재설정 링크와 만료 시간을 담은 메일을 클라이언트에 넘긴다.")
    void handsMailToClient() {
        passwordResetMailSender.send(EMAIL, RESET_LINK);

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(sesMailSender).send(org.mockito.ArgumentMatchers.eq(EMAIL), subject.capture(), body.capture());

        assertThat(subject.getValue()).contains("비밀번호 재설정");
        assertThat(body.getValue())
                .contains(RESET_LINK)
                .as("만료 시간은 도메인 상수를 따른다. 메일 문구와 정책이 어긋나면 안 된다")
                .contains(String.valueOf(PasswordResetTokenGenerator.TOKEN_TTL.toMinutes()));
    }
}
