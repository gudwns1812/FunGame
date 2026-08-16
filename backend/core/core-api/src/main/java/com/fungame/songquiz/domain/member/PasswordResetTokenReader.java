package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.PasswordResetTokenEntity;
import com.fungame.songquiz.storage.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenReader {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Transactional(readOnly = true)
    public Optional<Long> findMemberIdByTokenHash(String tokenHash) {
        return passwordResetTokenRepository.findMemberIdByTokenHash(tokenHash);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<PasswordResetToken> findByTokenHashForUpdate(String tokenHash) {
        return passwordResetTokenRepository.findByTokenHashForUpdate(tokenHash)
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
