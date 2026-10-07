package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.support.availability.TrafficGate;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpiredTokenCleaner {

    private static final String DAILY_CLEANUP_CRON = "0 0 4 * * *";

    private final PasswordResetService passwordResetService;
    private final TrafficGate trafficGate;

    @Scheduled(cron = DAILY_CLEANUP_CRON)
    public void deleteExpiredTokens() {
        if (!trafficGate.isAccepting()) {
            return;
        }

        passwordResetService.deleteExpiredTokens();
    }
}
