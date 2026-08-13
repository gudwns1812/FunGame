package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.CSQuizDifficulty;
import com.fungame.songquiz.storage.GameRoomStore;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import com.fungame.songquiz.support.lock.LockContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class GameRoomSettingsTest {

    private static final Long ROOM_ID = 1L;
    private static final GamePlayer HOST = GamePlayer.createNewPlayer(1L, "방장");
    private static final GamePlayer GUEST = GamePlayer.createNewPlayer(2L, "참가자");
    private static final GamePlayer GUEST2 = GamePlayer.createNewPlayer(3L, "참가자2");
    private static final RoomSettings SETTINGS =
            new RoomSettings(GameType.SONG, "방", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD);

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    @Mock
    GameTimer gameTimer;

    @Mock
    GameSessionManager gameSessionManager;

    @Mock
    GameRoomStore gameRoomStore;

    @Mock
    GameFactories gameFactories;

    GameRoomManager gameRoomManager;

    @BeforeEach
    void setUp() {
        gameRoomManager = new GameRoomManager(
                new LockContext(),
                applicationEventPublisher,
                gameTimer,
                gameSessionManager,
                gameRoomStore,
                gameFactories
        );
        gameRoomManager.createGameRoom(ROOM_ID, SETTINGS, HOST);
    }

    @Test
    @DisplayName("방장이 아니면 설정을 바꿀 수 없다.")
    void onlyHostCanChangeSettings() {
        gameRoomManager.joinRoom(ROOM_ID, GUEST);

        assertThatThrownBy(() -> gameRoomManager.changeSettings(ROOM_ID, GUEST.memberId(), SETTINGS))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.NOT_VALID_HOST);
    }

    @Test
    @DisplayName("게임이 진행 중이면 설정을 바꿀 수 없다.")
    void cannotChangeSettingsWhilePlaying() {
        gameRoomManager.startGame(ROOM_ID, HOST.memberId());

        assertThatThrownBy(() -> gameRoomManager.changeSettings(ROOM_ID, HOST.memberId(), SETTINGS))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.GAME_ALREADY_PLAYING);
    }

    @Test
    @DisplayName("정원을 현재 인원보다 작게 줄일 수 없다.")
    void cannotShrinkBelowCurrentPlayers() {
        gameRoomManager.joinRoom(ROOM_ID, GUEST);
        gameRoomManager.joinRoom(ROOM_ID, GUEST2);

        RoomSettings shrunk = SETTINGS.changeTo(GameType.SONG, 2, Category.KPOP, 10, 0, CSQuizDifficulty.HARD);

        assertThatThrownBy(() -> gameRoomManager.changeSettings(ROOM_ID, HOST.memberId(), shrunk))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.GAME_ROOM_MAX_PLAYER_EXCEED);
    }

    @Test
    @DisplayName("게임 종류를 바꾸면 다음 판은 바뀐 종류로 시작한다.")
    void canChangeGameType() {
        RoomSettings hangman = SETTINGS.changeTo(GameType.HANGMAN, 6, Category.DEFAULT, 1, 3, CSQuizDifficulty.HARD);

        gameRoomManager.changeSettings(ROOM_ID, HOST.memberId(), hangman);

        GameRoom room = gameRoomManager.findRoom(ROOM_ID);
        assertThat(room.getSettings().gameType()).isEqualTo(GameType.HANGMAN);
        assertThat(gameRoomManager.getGameType(ROOM_ID)).isEqualTo(GameType.HANGMAN);
    }

    @Test
    @DisplayName("설정을 바꿔도 방 이름은 유지된다.")
    void titleSurvivesSettingsChange() {
        gameRoomManager.changeSettings(ROOM_ID, HOST.memberId(), SETTINGS.changeTo(GameType.CS, 8, Category.DEFAULT, 5, 0, CSQuizDifficulty.NORMAL));

        assertThat(gameRoomManager.findRoom(ROOM_ID).getTitle()).isEqualTo(SETTINGS.title());
    }

    @Test
    @DisplayName("설정을 바꾸면 참가자의 준비 상태가 풀리고 방장만 준비 상태로 남는다.")
    void changingSettingsResetsReady() {
        gameRoomManager.joinRoom(ROOM_ID, GUEST);
        gameRoomManager.readyPlayer(ROOM_ID, GUEST.memberId());
        assertThat(gameRoomManager.findRoom(ROOM_ID).isAllReady()).isTrue();

        gameRoomManager.changeSettings(ROOM_ID, HOST.memberId(), SETTINGS.changeTo(GameType.SONG, 8, Category.POP, 5, 0, CSQuizDifficulty.HARD));

        GameRoom room = gameRoomManager.findRoom(ROOM_ID);
        assertThat(room.isAllReady()).isFalse();
        assertThat(room.getPlayers().snapshot())
                .filteredOn(player -> player.memberId().equals(HOST.memberId()))
                .allMatch(GamePlayer::isReady);
        assertThat(room.getSettings().category()).isEqualTo(Category.POP);
    }

    @Test
    @DisplayName("게임이 끝나면 방은 사라지지 않고 대기 상태로 돌아간다.")
    void endGameKeepsRoomInWaiting() {
        gameRoomManager.joinRoom(ROOM_ID, GUEST);
        gameRoomManager.readyPlayer(ROOM_ID, GUEST.memberId());
        gameRoomManager.startGame(ROOM_ID, HOST.memberId());

        gameRoomManager.endGame(ROOM_ID);

        GameRoom room = gameRoomManager.findRoom(ROOM_ID);
        assertThat(room.getStatus()).isEqualTo(GameRoomStatus.WAITING);
        assertThat(room.getRoomPlayers())
                .extracting(GamePlayer::memberId)
                .containsExactly(HOST.memberId(), GUEST.memberId());
        assertThat(room.isAllReady()).isFalse();
    }

    @Test
    @DisplayName("게임을 시작할 때마다 소진된 문제를 재사용하지 않도록 새 게임을 만든다.")
    void everyStartCreatesFreshGame() {
        Game firstGame = mock(Game.class);
        Game secondGame = mock(Game.class);
        org.mockito.BDDMockito.given(gameFactories.create(SETTINGS)).willReturn(firstGame, secondGame);

        gameRoomManager.startGame(ROOM_ID, HOST.memberId());
        assertThat(gameRoomManager.findRoom(ROOM_ID).getGame()).isSameAs(firstGame);

        gameRoomManager.endGame(ROOM_ID);
        assertThat(gameRoomManager.findRoom(ROOM_ID).getGame()).isNull();

        gameRoomManager.startGame(ROOM_ID, HOST.memberId());
        assertThat(gameRoomManager.findRoom(ROOM_ID).getGame()).isSameAs(secondGame);
    }
}
