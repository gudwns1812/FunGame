package com.fungame.songquiz.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fungame.songquiz.support.error.CoreException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameRoomTest {

    GameRoom gameRoom;

    @BeforeEach
    void setUp() {
        var songs = List.of(Song.of("불장난", "", Category.KPOP, LocalDate.of(2015, 1, 1), "", 30, List.of()));
        var songQuiz = new SongQuiz(songs);
        List<String> players = List.of("hi");
        gameRoom = GameRoom.create(songQuiz, players, 1, "hi");
    }

    @Test
    void 사용자의답변이_정답과_일치하면_정답처리한다() {
        //given
        String player = "hi";
        String answer = "불장난";
        //when
        boolean check = gameRoom.submitAnswer(player, answer);
        //then
        assertThat(check).isTrue();
        assertThat(gameRoom.getRank())
                .extracting(PlayerScore::score)
                .containsExactly(1);
    }

    @Test
    void 게임방의_최대플레이어를_초과하면_예외를_발생시킨다() {
        //given
        String player = "player";
        //when
        //then
        assertThatThrownBy(() -> gameRoom.addPlayer(player))
                .isInstanceOf(CoreException.class);
    }
}
