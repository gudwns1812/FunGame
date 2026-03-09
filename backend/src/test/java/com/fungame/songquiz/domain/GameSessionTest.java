package com.fungame.songquiz.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fungame.songquiz.domain.dto.GameSkipInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameSessionTest {

    GameSession gameSession;
    Game game;

    @BeforeEach
    void setUp() {
        game = mock(Game.class);
        List<String> players = List.of("p1", "p2", "p3");
        gameSession = new GameSession(game, players);
    }

    @Test
    @DisplayName("스킵 투표가 정족수(인원-1)에 도달하면 스킵 여부가 true가 된다.")
    void voteSkip_reaches_threshold() {
        // when
        gameSession.voteSkip("p1");
        GameSkipInfo info = gameSession.voteSkip("p2");

        // then
        assertThat(info.isSkip()).isTrue();
        assertThat(info.skipCount()).isEqualTo(2);
        assertThat(info.totalCount()).isEqualTo(2); // requiredCount
    }

    @Test
    @DisplayName("스킵 투표가 부족하면 스킵 여부는 false다.")
    void voteSkip_under_threshold() {
        // when
        GameSkipInfo info = gameSession.voteSkip("p1");

        // then
        assertThat(info.isSkip()).isFalse();
        assertThat(info.skipCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("라운드 종료 처리는 한 번만 성공해야 한다 (원자성).")
    void startProcessing_is_atomic() {
        // when
        boolean first = gameSession.startProcessing();
        boolean second = gameSession.startProcessing();

        // then
        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }
}
