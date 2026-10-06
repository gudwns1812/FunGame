package com.fungame.songquiz.domain.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.domain.quiz.Quiz;
import com.fungame.songquiz.domain.quiz.QuizFactoryRegistry;
import com.fungame.songquiz.domain.quiz.Song;
import com.fungame.songquiz.domain.quiz.SongQuiz;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoom;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.domain.room.RoomStore;
import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.storage.redis.GameRoomDao;
import com.fungame.songquiz.storage.redis.RedisTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DataRedisTest
@Import({RedisTestContainer.class, GameRoomDao.class})
class GameSessionManagerTest {

    private static final Long ROOM_ID = 1L;
    private static final GamePlayer HOST = GamePlayer.createNewPlayer(1L);
    private static final RoomSettings SETTINGS =
            new RoomSettings(GameType.SONG, "방", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GameRoomDao gameRoomDao;

    private final QuizFactoryRegistry quizFactoryRegistry = mock(QuizFactoryRegistry.class);

    private GameSessionManager gameSessionManager;
    private GameSessionManager otherInstance;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(redisTemplate.keys(GameRoomDao.KEY_PREFIX + "*"));
        gameSessionManager = managerOnAnotherInstance();
        otherInstance = managerOnAnotherInstance();
    }

    private GameSessionManager managerOnAnotherInstance() {
        return new GameSessionManager(quizFactoryRegistry, roomStore());
    }

    private RoomStore roomStore() {
        return new RoomStore(gameRoomDao, new ObjectMapper().findAndRegisterModules());
    }

    private void openRoomWithGame() {
        RoomStore store = roomStore();
        store.create(GameRoom.create(ROOM_ID, SETTINGS, HOST));
        store.updateTable(ROOM_ID, table -> {
            table.room().start(HOST.memberId());
            table.startGame(new GameSession(songQuiz(), table.room().getRoomPlayers()));
            return null;
        });
    }

    @Test
    @DisplayName("게임을 시작할 때마다 소진된 문제를 재사용하지 않도록 새 문제를 만든다.")
    void everyStartCreatesFreshQuiz() {
        Quiz firstQuiz = mock(Quiz.class);
        Quiz secondQuiz = mock(Quiz.class);
        given(quizFactoryRegistry.create(SETTINGS)).willReturn(firstQuiz, secondQuiz);

        assertThat(gameSessionManager.createQuiz(SETTINGS)).isSameAs(firstQuiz);
        assertThat(gameSessionManager.createQuiz(SETTINGS)).isSameAs(secondQuiz);
    }

    @Test
    @DisplayName("한 서버가 연 라운드에서 다른 서버가 정답을 받고 점수를 올린다. 판이 한 서버에 묶이지 않는다.")
    void anotherInstanceContinuesTheGame() {
        openRoomWithGame();
        gameSessionManager.update(ROOM_ID, game -> {
            game.startRound();
            return null;
        });

        ActionResult result = otherInstance.update(ROOM_ID,
                game -> game.handleAction(GameAction.submitAnswer(HOST.memberId(), "정답"))).orElseThrow();
        otherInstance.update(ROOM_ID, game -> {
            game.startProcessing();
            game.updatePlayerPoint(HOST.memberId());
            return null;
        });

        assertThat(result).isEqualTo(ActionResult.CORRECT);
        assertThat(gameSessionManager.find(ROOM_ID).orElseThrow().getPlayerRanks())
                .extracting(PlayerScore::score)
                .containsExactly(1);
    }

    @Test
    @DisplayName("두 서버가 같은 라운드를 동시에 끝내려 해도 한 곳만 끝낸다.")
    void onlyOneInstanceEndsTheRound() {
        openRoomWithGame();
        gameSessionManager.update(ROOM_ID, game -> {
            game.startRound();
            return null;
        });

        boolean first = gameSessionManager.update(ROOM_ID, GameSession::startProcessing).orElseThrow();
        boolean second = otherInstance.update(ROOM_ID, GameSession::startProcessing).orElseThrow();

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    @DisplayName("판이 없는 방이나 없는 방은 바꿀 판이 없다고 답한다.")
    void nothingToUpdateWithoutGame() {
        roomStore().create(GameRoom.create(ROOM_ID, SETTINGS, HOST));

        assertThat(gameSessionManager.update(ROOM_ID, GameSession::startProcessing)).isEmpty();
        assertThat(gameSessionManager.update(999L, GameSession::startProcessing)).isEmpty();
        assertThat(gameSessionManager.find(ROOM_ID)).isEmpty();
    }

    @Test
    @DisplayName("진행 중인 판의 수를 모든 서버 기준으로 센다.")
    void countsGamesAcrossInstances() {
        openRoomWithGame();

        assertThat(otherInstance.count()).isEqualTo(1);
    }

    private static SongQuiz songQuiz() {
        return new SongQuiz(List.of(Song.stored(10L, "정답", "가수", List.of(Category.KPOP),
                LocalDate.of(2020, 1, 1), "youtube.com/10", 30, List.of(), "힌트")), Category.KPOP);
    }
}
