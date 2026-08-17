package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.client.discord.DiscordEmbed;
import com.fungame.songquiz.client.discord.DiscordWebhookSender;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiscordReportNotifierTest {

    private static final Long MEMBER_ID = 3L;
    private static final Long ROOM_ID = 42L;
    private static final Long CONTENT_ID = 777L;

    @Mock
    private DiscordWebhookSender webhookSender;

    @InjectMocks
    private DiscordReportNotifier notifier;

    private static Report report() {
        return Report.open(MEMBER_ID, ReportSource.IN_GAME, ReportReason.ANSWER_WRONG, null,
                new ReportContext(GameType.SONG, "KPOP", CONTENT_ID, ROOM_ID, 2, 5,
                        "https://youtu.be/BzYnNdJhZQw", "아이유 - 밤편지", "아이유 - ㅂㅍㅈ"));
    }

    private DiscordEmbed sentEmbed() {
        ArgumentCaptor<DiscordEmbed> captor = ArgumentCaptor.forClass(DiscordEmbed.class);
        verify(webhookSender).send(captor.capture());

        return captor.getValue();
    }

    @Test
    @DisplayName("사유를 제목으로, 게임 컨텍스트를 필드로 담아 보낸다.")
    void sendsReasonAsTitleAndContextAsFields() {
        notifier.notifyReport(report());

        DiscordEmbed embed = sentEmbed();
        assertThat(embed.title()).isEqualTo(ReportSummary.of(report()).title());
        assertThat(embed.fields())
                .extracting(DiscordEmbed.Field::value)
                .contains("https://youtu.be/BzYnNdJhZQw", "아이유 - 밤편지", "2 / 5");
    }

    @Test
    @DisplayName("컨텍스트가 비어 있는 신고도 보낼 수 있다.")
    void sendsReportWithoutContext() {
        Report lobbyReport = Report.open(MEMBER_ID, ReportSource.LOBBY, ReportReason.ETC, "튕겨요",
                ReportContext.outsideGame(null, null));

        assertThatCode(() -> notifier.notifyReport(lobbyReport)).doesNotThrowAnyException();

        assertThat(sentEmbed().fields()).isNotEmpty();
    }
}
