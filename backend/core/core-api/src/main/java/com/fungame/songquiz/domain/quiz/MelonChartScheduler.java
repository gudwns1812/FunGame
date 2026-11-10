package com.fungame.songquiz.domain.quiz;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.melon-chart.enabled", matchIfMissing = true)
public class MelonChartScheduler {

    private static final String DAILY_NOON_CRON = "0 0 12 * * *";
    private static final String SEOUL = "Asia/Seoul";

    private final MelonChartCollector melonChartCollector;

    @Scheduled(cron = DAILY_NOON_CRON, zone = SEOUL)
    @SchedulerLock(name = "collectMelonChart", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void collectMelonChart() {
        melonChartCollector.collect();
    }
}
