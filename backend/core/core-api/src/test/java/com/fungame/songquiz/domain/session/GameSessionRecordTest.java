package com.fungame.songquiz.domain.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fungame.songquiz.domain.quiz.HangmanQuiz;
import com.fungame.songquiz.domain.quiz.HangmanWord;
import com.fungame.songquiz.domain.quiz.Song;
import com.fungame.songquiz.domain.quiz.SongQuiz;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameSessionRecordTest {

    private static final GamePlayer FIRST = GamePlayer.createNewPlayer(1L);
    private static final GamePlayer SECOND = GamePlayer.createNewPlayer(2L);
    private static final String NO_GAME_ID = null;
    private static final Instant NO_STARTED_AT = null;

    @Test
    void 점수제_판은_판_id_종류_시작_시각과_결과를_전적으로_낸다() {
        GameSession session = songSessionOf(FIRST, SECOND);
        session.updatePlayerPoint(FIRST.memberId());

        FinishedGame finished = session.scoredRecord().orElseThrow();

        assertThat(finished.gameId()).isEqualTo(session.getGameId().orElseThrow());
        assertThat(finished.gameType()).isEqualTo(GameType.SONG);
        assertThat(finished.startedAt()).isEqualTo(session.getStartedAt());
        assertThat(finished.outcome().players())
                .extracting(PlayerOutcome::memberId, PlayerOutcome::won)
                .containsExactly(
                        tuple(FIRST.memberId(), true),
                        tuple(SECOND.memberId(), false));
    }

    @Test
    void 협동_판은_성공_여부로_전적을_낸다() {
        HangmanQuiz quiz = HangmanQuiz.create(new HangmanWord(30L, "APPLE", 1));
        quiz.initPlayers(List.of(FIRST, SECOND));
        GameSession session = new GameSession(quiz, List.of(FIRST, SECOND));

        FinishedGame finished = session.cooperativeRecord(true).orElseThrow();

        assertThat(finished.gameType()).isEqualTo(GameType.HANGMAN);
        assertThat(finished.outcome().players()).extracting(PlayerOutcome::won).containsOnly(true);
    }

    @Test
    void 혼자_시작한_판은_전적을_내지_않는다() {
        GameSession session = songSessionOf(FIRST);

        assertThat(session.scoredRecord()).isEmpty();
    }

    @Test
    void 판_id_가_없는_옛_판은_전적을_내지_않는다() {
        GameSnapshot current = songSessionOf(FIRST, SECOND).snapshot();
        GameSnapshot beforeGameIds = new GameSnapshot(current.quiz(), current.participants(), current.skipVoters(),
                current.roundStartedAt(), NO_GAME_ID, NO_STARTED_AT);

        assertThat(GameSession.restore(beforeGameIds).scoredRecord()).isEmpty();
    }

    private static GameSession songSessionOf(GamePlayer... players) {
        Song song = Song.stored(10L, "첫곡", "가수", List.of(Category.KPOP), LocalDate.of(2020, 1, 1),
                "youtube.com/10", 30, List.of("별칭"), "힌트");
        return new GameSession(new SongQuiz(List.of(song), Category.KPOP), List.of(players));
    }
}
