package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.client.discord.DiscordEmbed;
import com.fungame.songquiz.client.discord.DiscordWebhookSender;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class DiscordReportNotifier implements ReportNotifier {

    private final DiscordWebhookSender webhookSender;

    @Async
    @Override
    public void notifyReport(Report report) {
        webhookSender.send(embedOf(ReportSummary.of(report)));
    }

    private static DiscordEmbed embedOf(ReportSummary summary) {
        List<DiscordEmbed.Field> fields = summary.lines().stream()
                .map(line -> new DiscordEmbed.Field(line.name(), line.value()))
                .toList();

        return new DiscordEmbed(summary.title(), fields);
    }
}
