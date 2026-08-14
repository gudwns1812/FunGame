package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.PasswordResetTokenEntity;
import com.fungame.songquiz.storage.PasswordResetTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenReader {

    private final PasswordResetTokenStore passwordResetTokenStore;

    public Optional<Long> findMemberIdByTokenHash(String tokenHash) {
        return passwordResetTokenStore.findMemberIdByTokenHash(tokenHash);
    }

    public Optional<PasswordResetToken> findByTokenHashForUpdate(String tokenHash) {
        return passwordResetTokenStore.findByTokenHashForUpdate(tokenHash)
                .map(PasswordResetTokenReader::toToken);
    }

    private static PasswordResetToken toToken(PasswordResetTokenEntity entity) {
        return PasswordResetToken.restore(
                entity.getId(),
                entity.getMember().getId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getUsedAt());
    }
}
