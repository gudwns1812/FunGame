package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int EXPIRED_TOKEN_RETENTION_DAYS = 1;
    private static final String DAILY_CLEANUP_CRON = "0 0 4 * * *";

    private final MemberReader memberReader;
    private final MemberWriter memberWriter;
    private final PasswordResetTokenReader passwordResetTokenReader;
    private final PasswordResetTokenWriter passwordResetTokenWriter;
    private final PasswordResetTokenGenerator passwordResetTokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    private final Clock clock;

    @Transactional
    public void requestReset(String loginId, String email) {
        Optional<Member> requested = memberReader.findByLoginIdAndEmail(loginId, email);
        if (requested.isEmpty()) {
            return;
        }

        Member member = lockMember(requested.get().getId());
        passwordResetTokenWriter.removeAllOf(member.getId());

        String rawToken = passwordResetTokenGenerator.generateRawToken();
        passwordResetTokenWriter.append(PasswordResetToken.issue(
                member.getId(),
                passwordResetTokenGenerator.hash(rawToken),
                now().plus(PasswordResetTokenGenerator.TOKEN_TTL)));

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(email, rawToken));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (!PasswordPolicy.isSatisfiedBy(newPassword)) {
            throw new CoreException(ErrorType.PASSWORD_POLICY_VIOLATION);
        }

        String tokenHash = passwordResetTokenGenerator.hash(rawToken);
        Long memberId = passwordResetTokenReader.findMemberIdByTokenHash(tokenHash)
                .orElseThrow(() -> new CoreException(ErrorType.INVALID_PASSWORD_RESET_TOKEN));

        Member member = lockMember(memberId);
        PasswordResetToken token = passwordResetTokenReader.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new CoreException(ErrorType.INVALID_PASSWORD_RESET_TOKEN));

        LocalDateTime now = now();
        if (!token.isUsable(now)) {
            throw new CoreException(ErrorType.INVALID_PASSWORD_RESET_TOKEN);
        }

        member.changePassword(passwordEncoder.encode(newPassword));
        memberWriter.update(member);

        token.markUsed(now);
        passwordResetTokenWriter.markUsed(token);

        expireEverySessionOf(member);
    }

    @Scheduled(cron = DAILY_CLEANUP_CRON)
    @Transactional
    public void deleteExpiredTokens() {
        passwordResetTokenWriter.removeExpiredBefore(now().minusDays(EXPIRED_TOKEN_RETENTION_DAYS));
    }

    private Member lockMember(Long memberId) {
        return memberReader.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.INVALID_PASSWORD_RESET_TOKEN));
    }

    private void expireEverySessionOf(Member member) {
        sessionRepository.findByPrincipalName(MemberAdapter.principalNameOf(member))
                .keySet()
                .forEach(sessionRepository::deleteById);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
