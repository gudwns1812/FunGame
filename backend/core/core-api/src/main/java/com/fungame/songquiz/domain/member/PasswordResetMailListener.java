package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.event.PasswordResetRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class PasswordResetMailListener {

    private static final String TOKEN_QUERY_PARAMETER = "token";

    private final PasswordResetMailSender mailSender;
    private final String linkBaseUrl;

    public PasswordResetMailListener(PasswordResetMailSender mailSender,
                                     @Value("${app.password-reset.link-base-url}") String linkBaseUrl) {
        this.mailSender = mailSender;
        this.linkBaseUrl = linkBaseUrl;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        try {
            mailSender.send(event.email(), resetLink(event.rawToken()));
        } catch (RuntimeException e) {
            log.error("비밀번호 재설정 메일 발송에 실패했습니다. 수신자: {}", event.email(), e);
        }
    }

    private String resetLink(String rawToken) {
        return UriComponentsBuilder.fromUriString(linkBaseUrl)
                .queryParam(TOKEN_QUERY_PARAMETER, rawToken)
                .build()
                .toUriString();
    }
}
