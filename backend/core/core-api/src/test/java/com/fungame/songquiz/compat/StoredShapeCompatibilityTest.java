package com.fungame.songquiz.compat;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.api.websocket.BroadcastMessage;
import com.fungame.songquiz.domain.quiz.Song;
import com.fungame.songquiz.domain.quiz.SongQuiz;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoom;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.domain.room.RoomSnapshot;
import com.fungame.songquiz.domain.session.GameSession;
import com.fungame.songquiz.domain.session.GameSnapshot;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.enums.GameType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * 버전이 섞여 도는 동안 저장 형태와 서버 간 메시지가 앞뒤로 읽히는지 지킨다.
 *
 * <p>무중단 배포는 서버를 하나씩 바꾸므로 몇 분 동안 옛 버전과 새 버전이 같은 Redis 를 함께 쓴다.
 * 필드 이름을 바꾸거나 지우면 상대 버전이 쓴 방을 읽지 못하고, 그 방에는 진행 중인 판이 들어 있다.
 *
 * <p>이 테스트가 깨지면 <b>형태를 한 번의 배포로 바꾸려 한 것</b>이다. 표본을 고쳐 넘기지 말고
 * 새 형태를 "있어도 되고 없어도 되는" 것으로 먼저 더해 한 번 배포하고, 옛 형태는 다음 배포에서 지운다.
 * {@code BACKEND.md} 의 "저장 형태와 스키마는 두 번에 나눠 바꾼다" 를 따른다.
 */
class StoredShapeCompatibilityTest {

    /**
     * 운영이 쓰는 매퍼와 같은 설정이다. 스프링 부트가 이 빌더로 {@code ObjectMapper} 빈을 만들고,
     * 그 빈이 {@code RoomStore} 로 주입된다. 맨손 {@code new ObjectMapper()} 는 모르는 필드를 만나면
     * 터지므로, 그걸로 검사하면 운영에서 읽히는 것을 읽지 못한다고 할 수 있다.
     */
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    @DisplayName("지난 버전이 쓴 방을 지금 코드가 읽는다.")
    void readsARoomWrittenByTheVersionBefore() throws Exception {
        GameRoom room = GameRoom.restore(objectMapper.readValue(sample("room.json"), RoomSnapshot.class));

        assertThat(room.getRoomId()).isEqualTo(7L);
        assertThat(room.getSettings().gameType()).isEqualTo(GameType.SONG);
        assertThat(room.getSettings().maxPlayers()).isEqualTo(8);
        assertThat(room.getStatus()).isEqualTo(GameRoomStatus.WAITING);
        assertThat(room.getRoomPlayers()).hasSize(2);
        assertThat(room.isHost(1L)).isTrue();
        // 이 값이 비면 유휴 방 정리가 그 방에서 터진다
        assertThat(room.isIdle(Instant.now())).isTrue();
    }

    @Test
    @DisplayName("지난 버전이 쓴 판을 지금 코드가 읽는다.")
    void readsAGameWrittenByTheVersionBefore() throws Exception {
        GameSession game = GameSession.restore(objectMapper.readValue(sample("game.json"), GameSnapshot.class));

        assertThat(game.getPlayerRanks()).hasSize(2);
        assertThat(game.hasPlayer(1L)).isTrue();
        assertThat(game.getTotalRound()).isEqualTo(1);
        // 퀴즈는 kind 필드로 종류를 가린다. 그 이름이나 값이 바뀌면 상대 버전이 판을 못 읽는다
        assertThat(game.getQuiz()).isInstanceOf(SongQuiz.class);
    }

    @Test
    @DisplayName("다음 버전이 더한 필드가 섞여 있어도 방을 읽는다. 필드를 더하는 것은 배포 한 번으로 해도 된다.")
    void readsARoomCarryingAFieldFromTheVersionAfter() throws Exception {
        String withNewerField = withAFieldFromTheVersionAfter(sample("room.json"));

        GameRoom room = GameRoom.restore(objectMapper.readValue(withNewerField, RoomSnapshot.class));

        assertThat(room.getRoomId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("다음 버전이 더한 필드가 섞여 있어도 판을 읽는다.")
    void readsAGameCarryingAFieldFromTheVersionAfter() throws Exception {
        String withNewerField = withAFieldFromTheVersionAfter(sample("game.json"));

        GameSession game = GameSession.restore(objectMapper.readValue(withNewerField, GameSnapshot.class));

        assertThat(game.getPlayerRanks()).hasSize(2);
    }

    @Test
    @DisplayName("지금 코드가 쓴 방에도 지난 버전이 읽던 필드가 전부 남아 있다.")
    void writesARoomTheVersionBeforeCanStillRead() throws Exception {
        JsonNode written = objectMapper.valueToTree(sampleShapedRoom().snapshot());

        assertEveryFieldSurvives(objectMapper.readTree(sample("room.json")), written, "방");
    }

    @Test
    @DisplayName("지금 코드가 쓴 판에도 지난 버전이 읽던 필드가 전부 남아 있다.")
    void writesAGameTheVersionBeforeCanStillRead() throws Exception {
        GameRoom room = sampleShapedRoom();
        GameSession game = new GameSession(sampleShapedQuiz(), room.getRoomPlayers());

        JsonNode written = objectMapper.valueToTree(game.snapshot());

        assertEveryFieldSurvives(objectMapper.readTree(sample("game.json")), written, "판");
    }

    /**
     * 읽기만 검사하면 모자란다. 필드를 더하면서 옛 필드를 <b>쓰지 않게</b> 바꾸면 읽기는 그대로 통과하고
     * 옛 서버만 깨진다. 그래서 지금 코드의 출력에 표본의 필드 이름과 구조가 남아 있는지 따로 본다.
     * 값은 보지 않는다 — 시각이나 식별자는 돌릴 때마다 달라진다.
     */
    private static void assertEveryFieldSurvives(JsonNode sample, JsonNode written, String where) {
        if (sample.isArray()) {
            if (!sample.isEmpty()) {
                assertThat(written.isArray() && !written.isEmpty())
                        .as("%s 의 %s 이 비었다. 옛 버전은 여기에 원소가 있다고 본다", where, sample)
                        .isTrue();
                assertEveryFieldSurvives(sample.get(0), written.get(0), where + "[0]");
            }
            return;
        }

        sample.fieldNames().forEachRemaining(name -> {
            JsonNode child = written.get(name);
            assertThat(child)
                    .as("%s 의 '%s' 를 더 이상 쓰지 않는다. 옛 버전은 이 이름으로 읽는다 — "
                            + "지우는 것은 다음 배포에서 한다", where, name)
                    .isNotNull();

            if (sample.get(name).isContainerNode()) {
                assertEveryFieldSurvives(sample.get(name), child, where + "." + name);
            }
        });
    }

    private static GameRoom sampleShapedRoom() {
        GameRoom room = GameRoom.create(7L,
                new RoomSettings(GameType.SONG, "방", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD),
                GamePlayer.createNewPlayer(1L));
        room.join(GamePlayer.createNewPlayer(2L));

        return room;
    }

    private static SongQuiz sampleShapedQuiz() {
        return new SongQuiz(List.of(Song.stored(10L, "정답", "가수", List.of(Category.KPOP),
                LocalDate.of(2020, 1, 1), "youtube.com/10", 30, List.of(), "힌트")), Category.KPOP);
    }

    @Test
    @DisplayName("서버 사이를 오가는 브로드캐스트는 이 필드 이름으로 실린다. 이름이 바뀌면 상대 버전이 읽지 못한다.")
    void broadcastTravelsUnderTheseFieldNames() {
        Map<String, String> fromTheVersionBefore = Map.of(
                "instance", "instance-a",
                "destination", "/topic/room/7",
                "user", "",
                "payload", "{\"round\":3}");

        BroadcastMessage message = BroadcastMessage.from(fromTheVersionBefore);

        assertThat(message.instanceId()).isEqualTo("instance-a");
        assertThat(message.destination()).isEqualTo("/topic/room/7");
        assertThat(message.payload()).isEqualTo("{\"round\":3}");
        assertThat(message.isForEveryone()).isTrue();
        assertThat(message.toFields()).isEqualTo(fromTheVersionBefore);
    }

    /** 다음 버전이 필드를 하나 더해 쓴 모양으로 만든다. */
    private static String withAFieldFromTheVersionAfter(String json) {
        return "{ \"somethingNewer\" : \"값\"," + json.substring(json.indexOf('{') + 1);
    }

    private static String sample(String name) throws IOException {
        return new ClassPathResource("compat/" + name)
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
