package com.fungame.songquiz.domain.member;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!prod")
public class LoggingPasswordResetMailSender implements PasswordResetMailSender {

    @Override
    public void send(String email, String resetLink) {
        log.info("비밀번호 재설정 링크 - 수신자: {}, 링크: {}", email, resetLink);
    }
}
