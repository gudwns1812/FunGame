package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.client.mail.SesMailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class SesPasswordResetMailSender implements PasswordResetMailSender {

    private static final String SUBJECT = "[FunGame] 비밀번호 재설정 안내";

    private final SesMailSender sesMailSender;

    @Override
    public void send(String email, String resetLink) {
        sesMailSender.send(email, SUBJECT, body(resetLink));
    }

    private String body(String resetLink) {
        return """
                아래 링크에서 새 비밀번호를 설정해주세요.

                %s

                링크는 %d분 뒤에 만료되고, 한 번만 사용할 수 있습니다.
                본인이 요청하지 않았다면 이 메일을 무시하세요.
                """.formatted(resetLink, PasswordResetTokenGenerator.TOKEN_TTL.toMinutes());
    }
}
