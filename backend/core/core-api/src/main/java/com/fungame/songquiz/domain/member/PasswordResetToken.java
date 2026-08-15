package com.fungame.songquiz.domain.member;

import lombok.Getter;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

@Getter
public class PasswordResetToken {

    private final Long id;
    private final Long memberId;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private LocalDateTime usedAt;

    private PasswordResetToken(Long id, Long memberId, String tokenHash, LocalDateTime expiresAt,
                              LocalDateTime usedAt) {
        this.id = id;
        this.memberId = memberId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
    }

    public static PasswordResetToken issue(Long memberId, String tokenHash, LocalDateTime expiresAt) {
        Assert.notNull(memberId, "회원은 필수입니다.");
        Assert.hasText(tokenHash, "토큰 해시는 필수입니다.");
        Assert.notNull(expiresAt, "만료 시각은 필수입니다.");

        return new PasswordResetToken(null, memberId, tokenHash, expiresAt, null);
    }

    public static PasswordResetToken restore(Long id, Long memberId, String tokenHash, LocalDateTime expiresAt,
                                             LocalDateTime usedAt) {
        return new PasswordResetToken(id, memberId, tokenHash, expiresAt, usedAt);
    }

    public boolean isUsable(LocalDateTime now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(LocalDateTime now) {
        this.usedAt = now;
    }
}
