package com.fungame.songquiz.domain;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameRoomTest {

    GameRoom gameRoom;
    Game game;

    @BeforeEach
    void setUp() {
        var songs = List.of(Song.of("정답", "", List.of(Category.KPOP), LocalDate.of(2015, 1, 1), "", 30, List.of(), ""));
        game = new SongQuiz(songs, Category.KPOP);
        List<String> players = new ArrayList<>(List.of("host"));
        gameRoom = GameRoom.create("방제목", game, players, 2, "host");
    }

    @Test
    @DisplayName("방에 플레이어가 정상적으로 입장한다.")
    void join_success() {
        // when
        int count = gameRoom.join("player2");

        // then
        assertThat(count).isEqualTo(2);
        assertThat(gameRoom.getRoomPlayers()).contains("player2");
    }

    @Test
    @DisplayName("최대 인원을 초과하여 입장하면 예외가 발생한다.")
    void join_fail_max_exceed() {
        // given
        gameRoom.join("player2");

        // when // then
        assertThatThrownBy(() -> gameRoom.join("player3"))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining(ErrorType.GAME_ROOM_MAX_PLAYER_EXCEED.getMessage());
    }

    @Test
    @DisplayName("게임이 이미 진행 중인 방에는 입장할 수 없다.")
    void join_fail_already_playing() {
        // given
        gameRoom.start("host");

        // when // then
        assertThatThrownBy(() -> gameRoom.join("player2"))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining(ErrorType.GAME_ALREADY_PLAYING.getMessage());
    }

    @Test
    @DisplayName("방장이 아닌 사용자가 게임을 시작하면 예외가 발생한다.")
    void start_fail_not_host() {
        // when // then
        assertThatThrownBy(() -> gameRoom.start("not_host"))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining(ErrorType.NOT_VALID_HOST.getMessage());
    }

    @Test
    @DisplayName("플레이어가 나갔을 때 방장이면 다음 사람에게 위임된다.")
    void leave_host_delegation() {
        // given
        gameRoom.join("player2");

        // when
        gameRoom.leave("host");

        // then
        assertThat(gameRoom.getPlayers().getHost()).isEqualTo("player2");
    }
}
