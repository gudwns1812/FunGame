package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.Quiz;
import com.fungame.songquiz.domain.quiz.QuizFactories;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GameSessionManagerTest {

    private static final Long ROOM_ID = 1L;
    private static final GamePlayer HOST = GamePlayer.createNewPlayer(1L, "host");
    private static final RoomSettings SETTINGS =
            new RoomSettings(GameType.SONG, "방", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD);

    private final QuizFactories quizFactories = mock(QuizFactories.class);
    private final GameSessionManager gameSessionManager = new GameSessionManager(quizFactories);

    @Test
    @DisplayName("게임을 시작할 때마다 소진된 문제를 재사용하지 않도록 새 게임을 만든다.")
    void everyStartCreatesFreshGame() {
        Quiz firstQuiz = mock(Quiz.class);
        Quiz secondQuiz = mock(Quiz.class);
        given(quizFactories.create(SETTINGS)).willReturn(firstQuiz, secondQuiz);

        GameSession first = gameSessionManager.startGame(ROOM_ID, SETTINGS, List.of(HOST));
        assertThat(first.getQuiz()).isSameAs(firstQuiz);

        gameSessionManager.endGameSession(ROOM_ID);
        assertThat(gameSessionManager.getGameSession(ROOM_ID)).isNull();

        GameSession second = gameSessionManager.startGame(ROOM_ID, SETTINGS, List.of(HOST));
        assertThat(second.getQuiz()).isSameAs(secondQuiz);
    }
}
