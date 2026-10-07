package com.fungame.songquiz.domain.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.domain.quiz.Song;
import com.fungame.songquiz.domain.quiz.SongQuiz;
import com.fungame.songquiz.domain.session.GameAction;
import com.fungame.songquiz.domain.session.GameSession;
import com.fungame.songquiz.domain.session.PlayerScore;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.storage.redis.GameRoomDao;
import com.fungame.songquiz.storage.redis.RedisTestContainer;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@DataRedisTest
@Import({RedisTestContainer.class, GameRoomDao.class})
class RoomStoreTest {

    private static final Long ROOM_ID = 7L;
    private static final Long UNREADABLE_ROOM_ID = 99L;
    private static final GamePlayer HOST = GamePlayer.createNewPlayer(1L);
    private static final GamePlayer GUEST = GamePlayer.createNewPlayer(2L);
    private static final RoomSettings SETTINGS =
            new RoomSettings(GameType.SONG, "방", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GameRoomDao gameRoomDao;

    private RoomStore store;
    private RoomStore otherInstance;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(redisTemplate.keys(GameRoomDao.KEY_PREFIX + "*"));
        store = storeOnAnotherInstance();
        otherInstance = storeOnAnotherInstance();
    }

    private RoomStore storeOnAnotherInstance() {
        return new RoomStore(gameRoomDao, new ObjectMapper().findAndRegisterModules());
    }

    private void openRoom(int maxPlayers) {
        store.create(GameRoom.create(ROOM_ID, withMaxPlayers(maxPlayers), HOST));
    }

    private static RoomSettings withMaxPlayers(int maxPlayers) {
        return SETTINGS.changeTo(SETTINGS.gameType(), maxPlayers, SETTINGS.category(), SETTINGS.totalRound(),
                SETTINGS.difficulty(), SETTINGS.csDifficulty());
    }

    @Test
    @DisplayName("한 서버가 만든 방을 다른 서버가 그대로 읽는다.")
    void anotherInstanceReadsTheRoom() {
        openRoom(8);

        GameRoom found = otherInstance.find(ROOM_ID).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("방");
        assertThat(found.getHostId()).isEqualTo(HOST.memberId());
        assertThat(otherInstance.findAll()).extracting(GameRoom::getRoomId).containsExactly(ROOM_ID);
    }

    @Test
    @DisplayName("다른 서버가 바꾼 방을 읽으면 바뀐 상태다.")
    void changesAreVisibleAcrossInstances() {
        openRoom(8);

        otherInstance.update(ROOM_ID, room -> room.join(GUEST));

        assertThat(store.find(ROOM_ID).orElseThrow().getRoomPlayers())
                .extracting(GamePlayer::memberId)
                .containsExactly(HOST.memberId(), GUEST.memberId());
    }

    @Test
    @DisplayName("회원이 어느 방에 있는지 들어오고 나갈 때마다 따라간다.")
    void memberIndexFollowsJoinAndLeave() {
        openRoom(8);
        assertThat(store.roomIdOf(HOST.memberId())).contains(ROOM_ID);

        store.update(ROOM_ID, room -> room.join(GUEST));
        assertThat(otherInstance.roomIdOf(GUEST.memberId())).contains(ROOM_ID);

        store.update(ROOM_ID, room -> {
            room.leave(GUEST.memberId());
            return null;
        });
        assertThat(otherInstance.roomIdOf(GUEST.memberId())).isEmpty();
    }

    @Test
    @DisplayName("마지막 사람이 나가 빈 방은 지워진다. 목록과 위치에서도 빠진다.")
    void emptiedRoomIsRemoved() {
        openRoom(8);

        store.update(ROOM_ID, room -> {
            room.leave(HOST.memberId());
            return null;
        });

        assertThat(otherInstance.find(ROOM_ID)).isEmpty();
        assertThat(otherInstance.findAll()).isEmpty();
        assertThat(otherInstance.roomIdOf(HOST.memberId())).isEmpty();
    }

    @Test
    @DisplayName("없는 방을 바꾸려 하면 방이 없다고 답한다.")
    void updatingMissingRoomFails() {
        assertThatThrownBy(() -> store.update(ROOM_ID, room -> room.join(GUEST)))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.GAME_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("두 서버가 동시에 바꿔도 어느 쪽 변경도 사라지지 않는다.")
    void concurrentChangesAreNotLost() throws Exception {
        openRoom(8);
        List<GamePlayer> guests = List.of(
                GamePlayer.createNewPlayer(11L), GamePlayer.createNewPlayer(12L), GamePlayer.createNewPlayer(13L),
                GamePlayer.createNewPlayer(14L), GamePlayer.createNewPlayer(15L), GamePlayer.createNewPlayer(16L));

        runConcurrently(guests.stream()
                .map(guest -> (Runnable) () -> storeOnAnotherInstance().update(ROOM_ID, room -> room.join(guest)))
                .toList());

        assertThat(store.find(ROOM_ID).orElseThrow().getPlayerCount()).isEqualTo(1 + guests.size());
    }

    @Test
    @DisplayName("한 자리 남은 방에 여럿이 동시에 들어오면 한 명만 들어온다. 정원을 넘지 않는다.")
    void capacityHoldsUnderConcurrentJoins() throws Exception {
        openRoom(2);
        AtomicInteger admitted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        runConcurrently(List.of(11L, 12L, 13L, 14L, 15L).stream()
                .map(memberId -> (Runnable) () -> {
                    try {
                        storeOnAnotherInstance().update(ROOM_ID,
                                room -> room.join(GamePlayer.createNewPlayer(memberId)));
                        admitted.incrementAndGet();
                    } catch (CoreException e) {
                        rejected.incrementAndGet();
                    }
                })
                .toList());

        assertThat(admitted.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(4);
        assertThat(store.find(ROOM_ID).orElseThrow().getPlayerCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("유휴한 방만 지운다. 여러 서버가 함께 지우려 해도 한 곳만 성공한다.")
    void removeIdleSucceedsOnce() {
        store.create(GameRoom.restore(new RoomSnapshot(ROOM_ID, SETTINGS, List.of(HOST), HOST.memberId(),
                GameRoomStatus.WAITING,
                Instant.now().minus(31, ChronoUnit.MINUTES), 0)));
        Instant threshold = Instant.now().minus(30, ChronoUnit.MINUTES);

        List<Long> first = store.removeIdle(threshold);
        List<Long> second = otherInstance.removeIdle(threshold);

        assertThat(first).containsExactly(ROOM_ID);
        assertThat(second).isEmpty();
        assertThat(store.find(ROOM_ID)).isEmpty();
    }

    @Test
    @DisplayName("활동이 있었던 방은 지우지 않는다.")
    void removeIdleKeepsActiveRoom() {
        openRoom(8);

        List<Long> removed = store.removeIdle(Instant.now().minus(30, ChronoUnit.MINUTES));

        assertThat(removed).isEmpty();
        assertThat(store.find(ROOM_ID)).isPresent();
    }

    @Test
    @DisplayName("판을 시작하면 방과 판이 한 번에 저장되고, 다른 서버가 그 판을 읽는다.")
    void gameIsStoredWithItsRoom() {
        openRoom(8);

        store.updateTable(ROOM_ID, table -> {
            GameSession game = new GameSession(songQuiz(), table.room().getRoomPlayers());
            game.startRound();
            table.startGame(game);
            return null;
        });

        RoomTable found = otherInstance.findTable(ROOM_ID).orElseThrow();
        assertThat(found.game()).isPresent();
        assertThat(found.game().orElseThrow().getCurrentRound()).isEqualTo(1);
    }

    @Test
    @DisplayName("한 서버가 시작한 판을 다른 서버가 이어서 바꾼다.")
    void anotherInstanceContinuesTheGame() {
        openRoom(8);
        startGame();

        otherInstance.updateTable(ROOM_ID, table -> {
            table.game().orElseThrow().updatePlayerPoint(HOST.memberId());
            return null;
        });

        assertThat(store.findTable(ROOM_ID).orElseThrow().game().orElseThrow().getPlayerRanks())
                .extracting(PlayerScore::score)
                .containsExactly(1);
    }

    @Test
    @DisplayName("아무것도 바꾸지 않은 변경은 쓰지 않는다. 오답 채팅마다 Redis 에 쓰지 않는다.")
    void unchangedTableIsNotWritten() {
        openRoom(8);
        startGame();
        String revisionBefore = revisionOfRoom();

        store.updateTable(ROOM_ID, table -> table.game().orElseThrow()
                .handleAction(GameAction.submitAnswer(HOST.memberId(), "오답")));

        assertThat(revisionOfRoom()).isEqualTo(revisionBefore);
    }

    @Test
    @DisplayName("다른 서버가 쓴 JSON 의 표기가 달라도 내용이 같으면 쓰지 않는다. 집합 순서처럼 서버마다 달라지는 표기로 쓰기가 늘지 않는다.")
    void sameContentWrittenDifferentlyIsNotWritten() throws Exception {
        openRoom(8);
        startGame();
        rewriteStoredJsonDifferently("body");
        rewriteStoredJsonDifferently("game");
        String revisionBefore = revisionOfRoom();

        store.updateTable(ROOM_ID, table -> table.game().orElseThrow()
                .handleAction(GameAction.submitAnswer(HOST.memberId(), "오답")));

        assertThat(revisionOfRoom()).isEqualTo(revisionBefore);
    }

    private void rewriteStoredJsonDifferently(String field) throws Exception {
        String roomKey = GameRoomDao.KEY_PREFIX + ROOM_ID;
        String stored = (String) redisTemplate.opsForHash().get(roomKey, field);
        ObjectMapper mapper = new ObjectMapper();
        String sameContent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(stored));
        redisTemplate.opsForHash().put(roomKey, field, sameContent);
    }

    @Test
    @DisplayName("판을 끝내면 판이 지워지고 방은 남는다.")
    void endingTheGameKeepsTheRoom() {
        openRoom(8);
        startGame();

        store.updateTable(ROOM_ID, table -> {
            table.endGame();
            return null;
        });

        RoomTable found = otherInstance.findTable(ROOM_ID).orElseThrow();
        assertThat(found.game()).isEmpty();
        assertThat(found.room().getRoomId()).isEqualTo(ROOM_ID);
    }

    private void startGame() {
        store.updateTable(ROOM_ID, table -> {
            GameSession game = new GameSession(songQuiz(), table.room().getRoomPlayers());
            game.startRound();
            table.startGame(game);
            return null;
        });
    }

    private String revisionOfRoom() {
        return (String) redisTemplate.opsForHash().get(GameRoomDao.KEY_PREFIX + ROOM_ID, "revision");
    }

    private static SongQuiz songQuiz() {
        return new SongQuiz(List.of(Song.stored(10L, "정답", "가수", List.of(Category.KPOP),
                LocalDate.of(2020, 1, 1), "youtube.com/10", 30, List.of(), "힌트")), Category.KPOP);
    }

    private static void runConcurrently(List<Runnable> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (Runnable task : tasks) {
                futures.add(executor.submit(() -> {
                    start.await();
                    task.run();
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }
    @Test
    @DisplayName("저장 형태를 읽을 수 없는 방은 목록에서 빠지고 나머지 방은 그대로 보인다.")
    void unreadableRoomIsLeftOutOfTheList() {
        openRoom(8);
        plantUnreadableRoom(UNREADABLE_ROOM_ID, Instant.now());

        assertThat(store.findAll()).extracting(GameRoom::getRoomId).containsExactly(ROOM_ID);
    }

    @Test
    @DisplayName("저장 형태를 읽을 수 없는 방은 없는 방으로 본다.")
    void unreadableRoomIsTreatedAsMissing() {
        plantUnreadableRoom(UNREADABLE_ROOM_ID, Instant.now());

        assertThat(store.find(UNREADABLE_ROOM_ID)).isEmpty();
        assertThatThrownBy(() -> store.update(UNREADABLE_ROOM_ID, room -> room.join(GUEST)))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getType())
                .isEqualTo(ErrorType.GAME_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("방금 쓰인 방은 읽지 못해도 지우지 않는다. 다음 버전이 쓴 멀쩡한 방일 수 있다.")
    void unreadableRoomWrittenRecentlyIsKept() {
        plantUnreadableRoom(UNREADABLE_ROOM_ID, Instant.now());

        List<Long> removed = store.removeIdle(Instant.now().minus(30, ChronoUnit.MINUTES));

        assertThat(removed).isEmpty();
        assertThat(redisTemplate.hasKey(GameRoomDao.KEY_PREFIX + UNREADABLE_ROOM_ID)).isTrue();
    }

    @Test
    @DisplayName("읽지 못하는 방은 마지막으로 쓰인 지 오래됐을 때 지운다.")
    void unreadableRoomIsRemovedOnlyAfterItsLastWriteIsOld() {
        openRoom(8);
        plantUnreadableRoom(UNREADABLE_ROOM_ID, Instant.now().minus(31, ChronoUnit.MINUTES));

        List<Long> removed = store.removeIdle(Instant.now().minus(30, ChronoUnit.MINUTES));

        assertThat(removed).containsExactly(UNREADABLE_ROOM_ID);
        assertThat(redisTemplate.hasKey(GameRoomDao.KEY_PREFIX + UNREADABLE_ROOM_ID)).isFalse();
        assertThat(store.findAll()).extracting(GameRoom::getRoomId).containsExactly(ROOM_ID);
    }

    @Test
    @DisplayName("쓰인 시각이 없는 방은 읽지 못해도 지우지 않는다. 이 필드를 모르는 옛 버전이 쓴 방이다.")
    void unreadableRoomWithoutWrittenTimeIsKept() {
        redisTemplate.opsForHash()
                .putAll(GameRoomDao.KEY_PREFIX + UNREADABLE_ROOM_ID, Map.of("body", "{\"roomId\":", "revision", "1"));
        redisTemplate.opsForSet().add(GameRoomDao.KEY_PREFIX + "ids", UNREADABLE_ROOM_ID.toString());

        List<Long> removed = store.removeIdle(Instant.now().minus(30, ChronoUnit.MINUTES));

        assertThat(removed).isEmpty();
        assertThat(redisTemplate.hasKey(GameRoomDao.KEY_PREFIX + UNREADABLE_ROOM_ID)).isTrue();
    }

    @Test
    @DisplayName("방을 만들면 쓰인 시각이 본문 밖에 남는다. 본문을 읽지 못해도 이 시각은 읽힌다.")
    void creatingARoomLeavesTheWrittenTimeOutsideTheBody() {
        Instant beforeCreate = Instant.now().minusSeconds(1);

        openRoom(8);

        assertThat(writtenTimeOf(ROOM_ID)).isAfter(beforeCreate);
    }

    @Test
    @DisplayName("방을 바꿀 때마다 쓰인 시각이 그 시점으로 밀린다. 갱신하지 않으면 멀쩡한 방이 유휴로 보여 지워진다.")
    void everyChangePushesTheWrittenTimeForward() {
        openRoom(8);
        Instant longAgo = Instant.now().minus(20, ChronoUnit.MINUTES);
        backdateWrittenTime(ROOM_ID, longAgo);

        store.update(ROOM_ID, room -> room.join(GUEST));

        assertThat(writtenTimeOf(ROOM_ID)).isAfter(longAgo.plus(10, ChronoUnit.MINUTES));
    }

    private Instant writtenTimeOf(Long roomId) {
        Object millis = redisTemplate.opsForHash().get(GameRoomDao.KEY_PREFIX + roomId, "updatedAt");
        return Instant.ofEpochMilli(Long.parseLong((String) millis));
    }

    private void backdateWrittenTime(Long roomId, Instant writtenAt) {
        redisTemplate.opsForHash()
                .put(GameRoomDao.KEY_PREFIX + roomId, "updatedAt", Long.toString(writtenAt.toEpochMilli()));
    }

    private void plantUnreadableRoom(Long roomId, Instant writtenAt) {
        redisTemplate.opsForHash().putAll(GameRoomDao.KEY_PREFIX + roomId, Map.of(
                "body", "{\"roomId\":",
                "revision", "1",
                "updatedAt", Long.toString(writtenAt.toEpochMilli())));
        redisTemplate.opsForSet().add(GameRoomDao.KEY_PREFIX + "ids", roomId.toString());
    }
}
