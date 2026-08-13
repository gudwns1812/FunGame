package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.CSQuizDifficulty;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameRoomTest {

    private static final GamePlayer HOST = GamePlayer.createNewPlayer(1L, "host");
    private static final GamePlayer PLAYER2 = GamePlayer.createNewPlayer(2L, "player2");
    private static final GamePlayer PLAYER3 = GamePlayer.createNewPlayer(3L, "player3");
    private static final Long NOT_HOST_ID = 99L;

    GameRoom gameRoom;
    Game game;

    @BeforeEach
    void setUp() {
        var songs = List.of(Song.of("정답", "", List.of(Category.KPOP), LocalDate.of(2015, 1, 1), "", 30, List.of(), ""));
        game = new SongQuiz(songs, Category.KPOP);
        gameRoom = GameRoom.create(new RoomSettings(GameType.SONG, "방제목", 2, Category.KPOP, 1, 0, CSQuizDifficulty.HARD), HOST);
    }

    @Test
    @DisplayName("방에 플레이어가 정상적으로 입장한다.")
    void join_success() {
        // when
        JoinResult result = gameRoom.join(PLAYER2);

        // then
        assertThat(result.playerNumber()).isEqualTo(2);
        assertThat(result.newlyJoined()).isTrue();
        assertThat(gameRoom.getRoomPlayers()).extracting(GamePlayer::memberId).contains(PLAYER2.memberId());
    }

    @Test
    @DisplayName("이미 방에 있는 플레이어가 다시 입장해도 새 참가로 집계되지 않는다.")
    void join_is_idempotent() {
        // given
        gameRoom.join(PLAYER2);

        // when
        JoinResult result = gameRoom.join(PLAYER2);

        // then
        assertThat(result.newlyJoined()).isFalse();
        assertThat(result.playerNumber()).isEqualTo(2);
        assertThat(gameRoom.getRoomPlayers())
                .extracting(GamePlayer::nickname)
                .containsExactly("host", "player2");
    }

    @Test
    @DisplayName("닉네임이 같아도 다른 회원이면 별개의 플레이어로 입장한다.")
    void join_distinguishes_same_nickname() {
        // given: 방장과 닉네임이 같지만 회원 번호가 다른 사람
        GamePlayer sameNickname = GamePlayer.createNewPlayer(42L, HOST.nickname());

        // when
        JoinResult result = gameRoom.join(sameNickname);

        // then
        assertThat(result.newlyJoined()).isTrue();
        assertThat(gameRoom.getRoomPlayers())
                .extracting(GamePlayer::memberId)
                .containsExactly(HOST.memberId(), sameNickname.memberId());
    }

    @Test
    @DisplayName("최대 인원을 초과하여 입장하면 예외가 발생한다.")
    void join_fail_max_exceed() {
        // given
        gameRoom.join(PLAYER2);

        // when // then
        assertThatThrownBy(() -> gameRoom.join(PLAYER3))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining(ErrorType.GAME_ROOM_MAX_PLAYER_EXCEED.getMessage());
    }

    @Test
    @DisplayName("게임이 이미 진행 중인 방에는 입장할 수 없다.")
    void join_fail_already_playing() {
        // given
        gameRoom.start(HOST.memberId(), game);

        // when // then
        assertThatThrownBy(() -> gameRoom.join(PLAYER2))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining(ErrorType.GAME_ALREADY_PLAYING.getMessage());
    }

    @Test
    @DisplayName("방장이 아닌 사용자가 게임을 시작하면 예외가 발생한다.")
    void start_fail_not_host() {
        // when // then
        assertThatThrownBy(() -> gameRoom.start(NOT_HOST_ID, game))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining(ErrorType.NOT_VALID_HOST.getMessage());
    }

    @Test
    @DisplayName("플레이어가 나갔을 때 방장이면 다음 사람에게 위임된다.")
    void leave_host_delegation() {
        // given
        gameRoom.join(PLAYER2);

        // when
        gameRoom.leave(HOST.memberId());

        // then
        assertThat(gameRoom.getPlayers().getHost()).isEqualTo(PLAYER2.memberId());
    }
}
