package com.fungame.songquiz.domain.member;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpiredTokenCleaner {

    private static final String DAILY_CLEANUP_CRON = "0 0 4 * * *";

    private final PasswordResetService passwordResetService;

    @Scheduled(cron = DAILY_CLEANUP_CRON)
    @SchedulerLock(name = "deleteExpiredTokens", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void deleteExpiredTokens() {
        passwordResetService.deleteExpiredTokens();
    }
}
