package com.fungame.songquiz.domain.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.domain.quiz.CsQuestion;
import com.fungame.songquiz.domain.quiz.CsQuiz;
import com.fungame.songquiz.domain.quiz.HangmanQuiz;
import com.fungame.songquiz.domain.quiz.HangmanWord;
import com.fungame.songquiz.domain.quiz.Song;
import com.fungame.songquiz.domain.quiz.SongQuiz;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.support.error.CoreException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameSessionSnapshotTest {

    private static final GamePlayer FIRST = GamePlayer.createNewPlayer(1L);
    private static final GamePlayer SECOND = GamePlayer.createNewPlayer(2L);
    private static final GamePlayer THIRD = GamePlayer.createNewPlayer(3L);

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private GameSession throughStorage(GameSession session) throws Exception {
        String stored = objectMapper.writeValueAsString(session.snapshot());

        return GameSession.restore(objectMapper.readValue(stored, GameSnapshot.class));
    }

    @Test
    @DisplayName("노래 퀴즈를 저장했다 되살리면 같은 라운드 · 같은 정답 · 같은 점수 · 같은 스킵 투표로 이어진다.")
    void songQuizSurvivesStorage() throws Exception {
        GameSession session = new GameSession(new SongQuiz(List.of(song(10, "첫곡"), song(11, "둘째곡")), Category.KPOP),
                List.of(FIRST, SECOND, THIRD));
        session.startRound();
        session.updatePlayerPoint(FIRST.memberId());
        session.handleAction(GameAction.skipVote(SECOND.memberId()));

        GameSession restored = throughStorage(session);

        assertThat(restored.getCurrentRound()).isEqualTo(1);
        assertThat(restored.getTotalRound()).isEqualTo(2);
        assertThat(restored.getCurrentContentId()).isEqualTo(10L);
        assertThat(restored.getAnswer()).isEqualTo(session.getAnswer());
        assertThat(restored.getHint()).isEqualTo(session.getHint());
        assertThat(restored.getPlayerRanks()).isEqualTo(session.getPlayerRanks());
        assertThat(restored.isSkipThresholdReached()).isFalse();
        assertThat(restored.handleAction(GameAction.skipVote(THIRD.memberId()))).isEqualTo(ActionResult.SKIP_VOTE_SUCCESS);
        assertThat(restored.handleAction(GameAction.submitAnswer(FIRST.memberId(), "첫곡"))).isEqualTo(ActionResult.CORRECT);
    }

    @Test
    @DisplayName("라운드 처리를 시작한 판은 되살려도 처리 중이다. 같은 라운드를 두 번 끝내지 않는다.")
    void roundProcessingSurvivesStorage() throws Exception {
        GameSession session = new GameSession(new SongQuiz(List.of(song(10, "첫곡")), Category.KPOP), List.of(FIRST));
        session.startRound();
        session.startProcessing();

        GameSession restored = throughStorage(session);

        assertThat(restored.startProcessing()).isFalse();
    }

    @Test
    @DisplayName("라운드 시작 시각이 저장돼 되살린 판도 남은 시간을 이어서 잰다.")
    void roundClockSurvivesStorage() throws Exception {
        GameSession session = new GameSession(new SongQuiz(List.of(song(10, "첫곡")), Category.KPOP), List.of(FIRST));
        session.startRound();

        GameSession restored = throughStorage(session);

        assertThat(restored.getRemainingRoundMillis())
                .isPositive()
                .isLessThanOrEqualTo(session.getRoundLength().toMillis());
    }

    @Test
    @DisplayName("판 도중에 나간 사람은 되살려도 빠져 있고, 다시 들어올 수 있다.")
    void leaverSurvivesStorage() throws Exception {
        GameSession session = new GameSession(new SongQuiz(List.of(song(10, "첫곡")), Category.KPOP),
                List.of(FIRST, SECOND));
        session.updatePlayerPoint(SECOND.memberId());
        session.removePlayer(SECOND.memberId());

        GameSession restored = throughStorage(session);

        assertThat(restored.hasPlayer(SECOND.memberId())).isFalse();
        assertThat(restored.canRejoin(SECOND.memberId())).isTrue();
        restored.restorePlayer(SECOND);
        assertThat(restored.getPlayerRanks()).extracting(PlayerScore::score).contains(1);
    }

    @Test
    @DisplayName("CS 퀴즈를 저장했다 되살리면 같은 문제와 정답으로 이어진다.")
    void csQuizSurvivesStorage() throws Exception {
        GameSession session = new GameSession(new CsQuiz(List.of(
                CsQuestion.of(20L, "네트워크", "TCP 는 몇 계층인가", List.of("4", "전송"), "해설", CSQuizDifficulty.NORMAL),
                CsQuestion.of(21L, "운영체제", "교착 상태 조건 수", List.of("4"), "해설", CSQuizDifficulty.HARD))),
                List.of(FIRST));
        session.startRound();

        GameSession restored = throughStorage(session);

        assertThat(restored.getContent()).isEqualTo(session.getContent());
        assertThat(restored.getAnswer()).isEqualTo(session.getAnswer());
        assertThat(restored.handleAction(GameAction.submitAnswer(FIRST.memberId(), "전송"))).isEqualTo(ActionResult.CORRECT);
        assertThat(restored.isLastRound()).isFalse();
    }

    @Test
    @DisplayName("행맨을 저장했다 되살리면 드러난 글자 · 틀린 글자 · 남은 기회 · 차례가 이어진다.")
    void hangmanSurvivesStorage() throws Exception {
        HangmanQuiz quiz = HangmanQuiz.create(new HangmanWord(30L, "APPLE", 1));
        quiz.initPlayers(List.of(FIRST, SECOND));
        GameSession session = new GameSession(quiz, List.of(FIRST, SECOND));
        quiz.guess(FIRST.memberId(), 'p');
        quiz.guess(SECOND.memberId(), 'z');

        GameSession restored = throughStorage(session);
        HangmanQuiz restoredQuiz = (HangmanQuiz) restored.getQuiz();

        assertThat(restoredQuiz.getCurrentDisplay()).isEqualTo(quiz.getCurrentDisplay());
        assertThat(restoredQuiz.getRemainingTries()).isEqualTo(quiz.getRemainingTries());
        assertThat(restoredQuiz.getCurrentTurnPlayer()).isEqualTo(FIRST);
        assertThat(restored.getContent()).isEqualTo(session.getContent());
        assertThatThrownBy(() -> restoredQuiz.guess(FIRST.memberId(), 'p'))
                .isInstanceOf(CoreException.class);
        assertThat(restoredQuiz.guess(FIRST.memberId(), 'a')).isEqualTo(ActionResult.ACTION_SUCCESS);
    }

    private static Song song(long id, String title) {
        return Song.stored(id, title, "가수", List.of(Category.KPOP), LocalDate.of(2020, 1, 1),
                "youtube.com/" + id, 30, List.of("별칭" + id), "힌트" + id);
    }
}
