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

    private HangmanQuiz quiz;
    private List<GamePlayer> players;

    @BeforeEach
    void setUp() {
        players = List.of(PLAYER1, PLAYER2);
        quiz = HangmanQuiz.create("APPLE");
        quiz.initPlayers(players);
    }

    @Test
    @DisplayName("정답 단어에 포함된 글자를 맞추면 표시 상태가 업데이트된다.")
    void guess_correct_letter() {
        // Given: player1의 턴
        // When
        ActionResult result = quiz.guess(PLAYER1.memberId(), 'A');

        // Then
        assertThat(result).isEqualTo(ActionResult.ACTION_SUCCESS);
        assertThat(quiz.getCurrentDisplay()).isEqualTo("A _ _ _ _");
        assertThat(quiz.getCurrentTurnIndex()).isEqualTo(1); // 턴이 넘어감
    }

    @Test
    @DisplayName("틀린 글자를 입력하면 기회가 차감되고 틀린 글자 목록에 추가된다.")
    void guess_wrong_letter() {
        // When
        ActionResult result = quiz.guess(PLAYER1.memberId(), 'Z');

        // Then
        assertThat(result).isEqualTo(ActionResult.ACTION_SUCCESS);
        assertThat(quiz.getRemainingTries()).isEqualTo(5);
        assertThat(quiz.getWrongLetters()).contains('Z');
        assertThat(quiz.getCurrentTurnIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("자신의 차례가 아닌 플레이어가 시도하면 예외가 발생한다.")
    void guess_not_turn() {
        // Given: player1의 턴일 때 player2가 시도
        // Then
        assertThatThrownBy(() -> quiz.guess(PLAYER2.memberId(), 'A'))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("이미 시도한 글자를 다시 시도하면 예외가 발생한다.")
    void guess_already_attempted() {
        // Given
        quiz.guess(PLAYER1.memberId(), 'A');
        
        // Then
        assertThatThrownBy(() -> quiz.guess(PLAYER2.memberId(), 'A'))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("단어를 모두 완성하면 승리(CORRECT) 처리된다.")
    void win_game() {
        // Given
        quiz.guess(PLAYER1.memberId(), 'A');
        quiz.guess(PLAYER2.memberId(), 'P');
        quiz.guess(PLAYER1.memberId(), 'L');
        
        // When: 마지막 글자 'E' 입력
        ActionResult result = quiz.guess(PLAYER2.memberId(), 'E');

        // Then
        assertThat(result).isEqualTo(ActionResult.CORRECT);
        assertThat(quiz.isGameWon()).isTrue();
    }

    @Test
    @DisplayName("기회를 모두 소진하면 패배(WRONG) 처리된다.")
    void lose_game() {
        // Given: 5번의 틀린 시도
        quiz.guess(PLAYER1.memberId(), 'Z');
        quiz.guess(PLAYER2.memberId(), 'Y');
        quiz.guess(PLAYER1.memberId(), 'X');
        quiz.guess(PLAYER2.memberId(), 'W');
        quiz.guess(PLAYER1.memberId(), 'V');

        // When: 마지막 6번째 틀린 시도
        ActionResult result = quiz.guess(PLAYER2.memberId(), 'U');

        // Then
        assertThat(result).isEqualTo(ActionResult.WRONG);
        assertThat(quiz.getRemainingTries()).isZero();
    }

    @Test
    @DisplayName("공백이 포함된 정답도 글자를 맞추면 표시 상태가 업데이트된다.")
    void guess_correct_letter_with_blank_answer() {
        // given
        quiz = HangmanQuiz.create("HOT DOG");
        quiz.initPlayers(players);

        // when
        ActionResult result = quiz.guess(PLAYER1.memberId(), 'O');

        // then
        assertThat(result).isEqualTo(ActionResult.ACTION_SUCCESS);
        assertThat(quiz.getCurrentDisplay()).isEqualTo("_ O _   _ O _");
    }

    @Test
    @DisplayName("공백이 포함된 정답을 모두 맞추면 승리 처리된다.")
    void win_game_with_blank_answer() {
        // given
        quiz = HangmanQuiz.create("HOT DOG");
        quiz.initPlayers(players);

        // when
        quiz.guess(PLAYER1.memberId(), 'H');
        quiz.guess(PLAYER2.memberId(), 'O');
        quiz.guess(PLAYER1.memberId(), 'T');
        quiz.guess(PLAYER2.memberId(), 'D');
        ActionResult result = quiz.guess(PLAYER1.memberId(), 'G');

        // then
        assertThat(result).isEqualTo(ActionResult.CORRECT);
        assertThat(quiz.isGameWon()).isTrue();
        assertThat(quiz.getCurrentDisplay()).isEqualTo("H O T   D O G");
    }

    @Test
    @DisplayName("자기 차례인 플레이어가 이탈하면 턴이 다음 사람에게 넘어간다.")
    void removePlayer_current_turn_moves_on() {
        // given: player1, player2, player3 중 player1 차례
        quiz = HangmanQuiz.create("APPLE");
        quiz.initPlayers(List.of(PLAYER1, PLAYER2, PLAYER3));

        // when
        quiz.dropPlayer(PLAYER1.memberId());

        // then
        assertThat(quiz.getPlayerOrder())
                .extracting(GamePlayer::nickname)
                .containsExactly("player2", "player3");
        assertThat(quiz.getCurrentTurnPlayer().nickname()).isEqualTo("player2");
    }

    @Test
    @DisplayName("앞 순번 플레이어가 이탈해도 현재 차례인 사람은 그대로 유지된다.")
    void removePlayer_before_current_keeps_turn() {
        // given: player1, player2, player3 에서 player2 차례로 진행
        quiz = HangmanQuiz.create("APPLE");
        quiz.initPlayers(List.of(PLAYER1, PLAYER2, PLAYER3));
        quiz.guess(PLAYER1.memberId(), 'A');
        assertThat(quiz.getCurrentTurnPlayer().nickname()).isEqualTo("player2");

        // when
        quiz.dropPlayer(PLAYER1.memberId());

        // then
        assertThat(quiz.getCurrentTurnPlayer().nickname()).isEqualTo("player2");
    }

    @Test
    @DisplayName("마지막 순번 플레이어가 자기 차례에 이탈하면 턴이 처음으로 돌아간다.")
    void removePlayer_last_index_wraps() {
        // given: player1, player2 에서 player2 차례
        quiz.guess(PLAYER1.memberId(), 'A');
        assertThat(quiz.getCurrentTurnPlayer().nickname()).isEqualTo("player2");

        // when
        quiz.dropPlayer(PLAYER2.memberId());

        // then
        assertThat(quiz.getCurrentTurnPlayer().nickname()).isEqualTo("player1");
    }
}
