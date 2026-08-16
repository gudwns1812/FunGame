package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HangmanGameTest {

    private static final GamePlayer PLAYER1 = GamePlayer.createNewPlayer(1L, "player1");
    private static final GamePlayer PLAYER2 = GamePlayer.createNewPlayer(2L, "player2");
    private static final GamePlayer PLAYER3 = GamePlayer.createNewPlayer(3L, "player3");

    private HangmanGame game;
    private List<GamePlayer> players;

    @BeforeEach
    void setUp() {
        players = List.of(PLAYER1, PLAYER2);
        game = HangmanGame.create("APPLE");
        game.initPlayers(players);
    }

    @Test
    @DisplayName("정답 단어에 포함된 글자를 맞추면 표시 상태가 업데이트된다.")
    void guess_correct_letter() {
        // Given: player1의 턴
        // When
        ActionResult result = game.guess(PLAYER1.memberId(), 'A');

        // Then
        assertThat(result).isEqualTo(ActionResult.ACTION_SUCCESS);
        assertThat(game.getCurrentDisplay()).isEqualTo("A _ _ _ _");
        assertThat(game.getCurrentTurnIndex()).isEqualTo(1); // 턴이 넘어감
    }

    @Test
    @DisplayName("틀린 글자를 입력하면 기회가 차감되고 틀린 글자 목록에 추가된다.")
    void guess_wrong_letter() {
        // When
        ActionResult result = game.guess(PLAYER1.memberId(), 'Z');

        // Then
        assertThat(result).isEqualTo(ActionResult.ACTION_SUCCESS);
        assertThat(game.getRemainingTries()).isEqualTo(5);
        assertThat(game.getWrongLetters()).contains('Z');
        assertThat(game.getCurrentTurnIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("자신의 차례가 아닌 플레이어가 시도하면 예외가 발생한다.")
    void guess_not_turn() {
        // Given: player1의 턴일 때 player2가 시도
        // Then
        assertThatThrownBy(() -> game.guess(PLAYER2.memberId(), 'A'))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("이미 시도한 글자를 다시 시도하면 예외가 발생한다.")
    void guess_already_attempted() {
        // Given
        game.guess(PLAYER1.memberId(), 'A');
        
        // Then
        assertThatThrownBy(() -> game.guess(PLAYER2.memberId(), 'A'))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("단어를 모두 완성하면 승리(CORRECT) 처리된다.")
    void win_game() {
        // Given
        game.guess(PLAYER1.memberId(), 'A');
        game.guess(PLAYER2.memberId(), 'P');
        game.guess(PLAYER1.memberId(), 'L');
        
        // When: 마지막 글자 'E' 입력
        ActionResult result = game.guess(PLAYER2.memberId(), 'E');

        // Then
        assertThat(result).isEqualTo(ActionResult.CORRECT);
        assertThat(game.isGameWon()).isTrue();
    }

    @Test
    @DisplayName("기회를 모두 소진하면 패배(WRONG) 처리된다.")
    void lose_game() {
        // Given: 5번의 틀린 시도
        game.guess(PLAYER1.memberId(), 'Z');
        game.guess(PLAYER2.memberId(), 'Y');
        game.guess(PLAYER1.memberId(), 'X');
        game.guess(PLAYER2.memberId(), 'W');
        game.guess(PLAYER1.memberId(), 'V');

        // When: 마지막 6번째 틀린 시도
        ActionResult result = game.guess(PLAYER2.memberId(), 'U');

        // Then
        assertThat(result).isEqualTo(ActionResult.WRONG);
        assertThat(game.getRemainingTries()).isZero();
    }

    @Test
    @DisplayName("공백이 포함된 정답도 글자를 맞추면 표시 상태가 업데이트된다.")
    void guess_correct_letter_with_blank_answer() {
        // given
        game = HangmanGame.create("HOT DOG");
        game.initPlayers(players);

        // when
        ActionResult result = game.guess(PLAYER1.memberId(), 'O');

        // then
        assertThat(result).isEqualTo(ActionResult.ACTION_SUCCESS);
        assertThat(game.getCurrentDisplay()).isEqualTo("_ O _   _ O _");
    }

    @Test
    @DisplayName("공백이 포함된 정답을 모두 맞추면 승리 처리된다.")
    void win_game_with_blank_answer() {
        // given
        game = HangmanGame.create("HOT DOG");
        game.initPlayers(players);

        // when
        game.guess(PLAYER1.memberId(), 'H');
        game.guess(PLAYER2.memberId(), 'O');
        game.guess(PLAYER1.memberId(), 'T');
        game.guess(PLAYER2.memberId(), 'D');
        ActionResult result = game.guess(PLAYER1.memberId(), 'G');

        // then
        assertThat(result).isEqualTo(ActionResult.CORRECT);
        assertThat(game.isGameWon()).isTrue();
        assertThat(game.getCurrentDisplay()).isEqualTo("H O T   D O G");
    }

    @Test
    @DisplayName("자기 차례인 플레이어가 이탈하면 턴이 다음 사람에게 넘어간다.")
    void removePlayer_current_turn_moves_on() {
        // given: player1, player2, player3 중 player1 차례
        game = HangmanGame.create("APPLE");
        game.initPlayers(List.of(PLAYER1, PLAYER2, PLAYER3));

        // when
        game.dropPlayer(PLAYER1.memberId());

        // then
        assertThat(game.getPlayerOrder())
                .extracting(GamePlayer::nickname)
                .containsExactly("player2", "player3");
        assertThat(game.getCurrentTurnPlayer().nickname()).isEqualTo("player2");
    }

    @Test
    @DisplayName("앞 순번 플레이어가 이탈해도 현재 차례인 사람은 그대로 유지된다.")
    void removePlayer_before_current_keeps_turn() {
        // given: player1, player2, player3 에서 player2 차례로 진행
        game = HangmanGame.create("APPLE");
        game.initPlayers(List.of(PLAYER1, PLAYER2, PLAYER3));
        game.guess(PLAYER1.memberId(), 'A');
        assertThat(game.getCurrentTurnPlayer().nickname()).isEqualTo("player2");

        // when
        game.dropPlayer(PLAYER1.memberId());

        // then
        assertThat(game.getCurrentTurnPlayer().nickname()).isEqualTo("player2");
    }

    @Test
    @DisplayName("마지막 순번 플레이어가 자기 차례에 이탈하면 턴이 처음으로 돌아간다.")
    void removePlayer_last_index_wraps() {
        // given: player1, player2 에서 player2 차례
        game.guess(PLAYER1.memberId(), 'A');
        assertThat(game.getCurrentTurnPlayer().nickname()).isEqualTo("player2");

        // when
        game.dropPlayer(PLAYER2.memberId());

        // then
        assertThat(game.getCurrentTurnPlayer().nickname()).isEqualTo("player1");
    }
}
