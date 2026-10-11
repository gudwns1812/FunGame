package com.fungame.songquiz.domain.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fungame.songquiz.enums.GameType;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameRecordSummaryTest {

    private static final Integer NO_WIN_RATE = null;

    @Test
    void 한_판도_하지_않은_회원도_노래_CS_행맨_세_줄을_순서대로_받는다() {
        GameRecordSummary summary = GameRecordSummary.of(List.of());

        assertThat(summary.byGameType())
                .extracting(GameTypeRecord::gameType, GameTypeRecord::played, GameTypeRecord::won,
                        GameTypeRecord::lost, GameTypeRecord::winRate)
                .containsExactly(
                        tuple(GameType.SONG, 0L, 0L, 0L, NO_WIN_RATE),
                        tuple(GameType.CS, 0L, 0L, 0L, NO_WIN_RATE),
                        tuple(GameType.HANGMAN, 0L, 0L, 0L, NO_WIN_RATE));
    }

    @Test
    void 센_전적은_그_게임_줄에만_들어가고_패는_판수에서_승을_뺀_것이다() {
        GameRecordSummary summary = GameRecordSummary.of(List.of(new GameTypeRecord(GameType.HANGMAN, 4, 1)));

        assertThat(summary.byGameType())
                .extracting(GameTypeRecord::gameType, GameTypeRecord::played, GameTypeRecord::won,
                        GameTypeRecord::lost)
                .containsExactly(
                        tuple(GameType.SONG, 0L, 0L, 0L),
                        tuple(GameType.CS, 0L, 0L, 0L),
                        tuple(GameType.HANGMAN, 4L, 1L, 3L));
    }

    @Test
    void 승률은_반올림한_백분율이다() {
        assertThat(new GameTypeRecord(GameType.SONG, 3, 2).winRate()).isEqualTo(67);
        assertThat(new GameTypeRecord(GameType.SONG, 3, 1).winRate()).isEqualTo(33);
        assertThat(new GameTypeRecord(GameType.SONG, 2, 2).winRate()).isEqualTo(100);
        assertThat(new GameTypeRecord(GameType.SONG, 5, 0).winRate()).isZero();
    }
}
