package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fungame.songquiz.api.controller.response.ApiResponse;
import com.fungame.songquiz.api.controller.response.RoomStateResponse;
import com.fungame.songquiz.domain.member.MemberProfileCache;
import com.fungame.songquiz.domain.member.MemberReader;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.PlayerJoinEvent;
import com.fungame.songquiz.domain.room.PlayerLeaveEvent;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.domain.room.RoomSettingsChangedEvent;
import com.fungame.songquiz.domain.room.RoomStateInfo;
import com.fungame.songquiz.domain.session.GameResultEvent;
import com.fungame.songquiz.domain.session.PlayerScore;
import com.fungame.songquiz.domain.session.ResultRow;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.MemberFixture;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

@DisplayName("방 이벤트는 델타가 아니라 바뀐 뒤의 방 전체를 싣는다")
class GameNotifierTest {

    private static final Long ROOM_ID = 7L;
    private static final GamePlayer HOST = new GamePlayer(1L, true);
    private static final GamePlayer GUEST = new GamePlayer(2L, false);

    private final StompBroadcaster broadcaster = mock(StompBroadcaster.class);
    private final MemberReader memberReader = mock(MemberReader.class);
    private final CacheManager cacheManager = new ConcurrentMapCacheManager(MemberProfileCache.CACHE_NAME);
    private final MemberProfileCache memberProfileCache = new MemberProfileCache(memberReader, cacheManager);
    private final GameNotifier gameNotifier = new GameNotifier(broadcaster, memberProfileCache);

    @BeforeEach
    void nameEveryone() {
        memberProfileCache.refresh(MemberFixture.withId(HOST.memberId(), "방장"));
        memberProfileCache.refresh(MemberFixture.withId(GUEST.memberId(), "참가자"));
    }

    @Test
    @DisplayName("입장 이벤트에 참가자 전체와 version 이 실린다.")
    void playerJoinCarriesWholeRoom() {
        gameNotifier.handlePlayerJoin(new PlayerJoinEvent(ROOM_ID, GUEST, state(3)));

        Map<String, Object> payload = capturedPayload();
        assertThat(payload).containsEntry("type", "PLAYER_JOIN")
                .containsEntry("memberId", GUEST.memberId())
                .containsEntry("nickname", "참가자");
        assertThat(payload.get("room")).isEqualTo(RoomStateResponse.from(state(3), memberProfileCache));
    }

    @Test
    @DisplayName("퇴장 이벤트도 같은 모양으로 방 전체를 싣는다.")
    void playerLeaveCarriesWholeRoom() {
        gameNotifier.handlePlayerLeave(new PlayerLeaveEvent(ROOM_ID, GUEST, state(4)));

        Map<String, Object> payload = capturedPayload();
        assertThat(payload).containsEntry("type", "PLAYER_LEAVE");
        assertThat(payload.get("room")).isEqualTo(RoomStateResponse.from(state(4), memberProfileCache));
    }

    @Test
    @DisplayName("설정 변경 이벤트는 설정과 방 전체를 함께 싣는다. 준비 상태가 초기화되기 때문이다.")
    void settingsChangeCarriesSettingsAndRoom() {
        gameNotifier.handleRoomSettingsChanged(new RoomSettingsChangedEvent(ROOM_ID, state(5)));

        Map<String, Object> payload = capturedPayload();
        assertThat(payload).containsEntry("type", "ROOM_SETTINGS_CHANGED").containsKey("settings");
        assertThat(payload.get("room")).isEqualTo(RoomStateResponse.from(state(5), memberProfileCache));
    }

    @Test
    @DisplayName("게임 결과의 순위표에 각자의 순위와 우승 여부가 실린다. 동점자는 같은 순위다.")
    void gameResultCarriesRankAndWinner() {
        gameNotifier.handleGameResult(new GameResultEvent(ROOM_ID, ResultRow.listOf(List.of(
                new PlayerScore(HOST, 3, 1),
                new PlayerScore(GUEST, 3, 1)))));

        assertThat(capturedRankings())
                .extracting(row -> row.get("nickname"), row -> row.get("score"), row -> row.get("rank"),
                        row -> row.get("winner"))
                .containsExactly(tuple("방장", 3, 1, true), tuple("참가자", 3, 1, true));
    }

    @Test
    @DisplayName("행맨 결과 행에는 순위가 없고 우승자도 아니다.")
    void hangmanResultHasNoRank() {
        gameNotifier.handleGameResult(new GameResultEvent(ROOM_ID, List.of(ResultRow.labelled("성공", 4))));

        assertThat(capturedRankings())
                .extracting(row -> row.get("nickname"), row -> row.get("rank"), row -> row.get("winner"))
                .containsExactly(tuple("성공", null, false));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> capturedRankings() {
        return (List<Map<String, Object>>) capturedPayload().get("rankings");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedPayload() {
        ArgumentCaptor<ApiResponse<Object>> captor = ArgumentCaptor.forClass(ApiResponse.class);
        verify(broadcaster).send(eq(StompDestination.room(ROOM_ID)), captor.capture());

        return (Map<String, Object>) captor.getValue().getData();
    }

    private static RoomStateInfo state(long version) {
        return new RoomStateInfo(ROOM_ID, version, GameRoomStatus.WAITING,
                new RoomSettings(GameType.SONG, "방 제목", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD),
                List.of(HOST, GUEST), HOST);
    }
}
