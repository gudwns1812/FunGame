package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.storage.PasswordResetTokenEntity;
import com.fungame.songquiz.storage.PasswordResetTokenRepository;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenWriter {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MemberRepository memberRepository;

    public void append(PasswordResetToken token) {
        passwordResetTokenRepository.save(PasswordResetTokenEntity.issue(
                memberRepository.getReferenceById(token.getMemberId()),
                token.getTokenHash(),
                token.getExpiresAt()));
    }

    public void markUsed(PasswordResetToken token) {
        PasswordResetTokenEntity entity = passwordResetTokenRepository.findById(token.getId())
                .orElseThrow(() -> new CoreException(ErrorType.INVALID_PASSWORD_RESET_TOKEN));

        entity.markUsed(token.getUsedAt());
    }

    public void removeAllOf(Long memberId) {
        passwordResetTokenRepository.deleteAllByMemberId(memberId);
    }

    public void removeExpiredBefore(LocalDateTime threshold) {
        passwordResetTokenRepository.deleteAllExpiredBefore(threshold);
    }
}
