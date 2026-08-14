package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.MemberStore;
import com.fungame.songquiz.storage.PasswordResetTokenEntity;
import com.fungame.songquiz.storage.PasswordResetTokenStore;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenWriter {

    private final PasswordResetTokenStore passwordResetTokenStore;
    private final MemberStore memberStore;

    public void append(PasswordResetToken token) {
        passwordResetTokenStore.save(PasswordResetTokenEntity.issue(
                memberStore.getReferenceById(token.getMemberId()),
                token.getTokenHash(),
                token.getExpiresAt()));
    }

    public void markUsed(PasswordResetToken token) {
        PasswordResetTokenEntity entity = passwordResetTokenStore.findById(token.getId())
                .orElseThrow(() -> new CoreException(ErrorType.INVALID_PASSWORD_RESET_TOKEN));

        entity.markUsed(token.getUsedAt());
    }

    public void removeAllOf(Long memberId) {
        passwordResetTokenStore.deleteAllByMemberId(memberId);
    }

    public void removeExpiredBefore(LocalDateTime threshold) {
        passwordResetTokenStore.deleteAllExpiredBefore(threshold);
    }
}
