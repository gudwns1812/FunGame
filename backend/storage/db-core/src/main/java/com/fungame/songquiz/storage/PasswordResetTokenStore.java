package com.fungame.songquiz.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenStore {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public Optional<PasswordResetTokenEntity> findById(Long id) {
        return passwordResetTokenRepository.findById(id);
    }

    public PasswordResetTokenEntity save(PasswordResetTokenEntity entity) {
        return passwordResetTokenRepository.save(entity);
    }

    public Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash) {
        return passwordResetTokenRepository.findByTokenHash(tokenHash);
    }

    public Optional<Long> findMemberIdByTokenHash(String tokenHash) {
        return passwordResetTokenRepository.findMemberIdByTokenHash(tokenHash);
    }

    public Optional<PasswordResetTokenEntity> findByTokenHashForUpdate(String tokenHash) {
        return passwordResetTokenRepository.findByTokenHashForUpdate(tokenHash);
    }

    public void deleteAllByMemberId(Long memberId) {
        passwordResetTokenRepository.deleteAllByMemberId(memberId);
    }

    public void deleteAllExpiredBefore(LocalDateTime threshold) {
        passwordResetTokenRepository.deleteAllExpiredBefore(threshold);
    }
}
