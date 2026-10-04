package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.controller.request.ChatRequest;
import com.fungame.songquiz.controller.response.ApiResponse;
import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.member.MemberProfiles;
import com.fungame.songquiz.domain.member.MemberReader;
import com.fungame.songquiz.domain.room.GameRoomManager;
import com.fungame.songquiz.domain.session.GameService;
import com.fungame.songquiz.support.MemberFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("채팅에 실리는 닉네임은 소켓을 연 시점이 아니라 지금 값이다")
class ChatControllerTest {

    private static final Long ROOM_ID = 7L;
    private static final Long MEMBER_ID = 1L;
    private static final String OLD_NICKNAME = "반달";
    private static final String NEW_NICKNAME = "보름달";

    private final StompBroadcaster broadcaster = mock(StompBroadcaster.class);
    private final MemberReader memberReader = mock(MemberReader.class);
    private final CacheManager cacheManager = new ConcurrentMapCacheManager(MemberProfiles.CACHE_NAME);
    private final MemberProfiles memberProfiles = new MemberProfiles(memberReader, cacheManager);
    private final ChatController chatController = new ChatController(
            broadcaster,
            mock(GameRoomManager.class),
            mock(GameService.class),
            memberProfiles);

    @Test
    @DisplayName("닉네임을 캐시에서 읽어 싣는다.")
    void carryNicknameFromCache() {
        given(memberReader.findMember(MEMBER_ID)).willReturn(member(OLD_NICKNAME));

        chatController.chat(ROOM_ID, principalWith(OLD_NICKNAME), new ChatRequest("안녕"));

        assertThat(capturedPayload()).containsEntry("memberId", MEMBER_ID)
                .containsEntry("nickname", OLD_NICKNAME)
                .containsEntry("message", "안녕");
    }

    @Test
    @DisplayName("프린시펄이 옛 닉네임을 들고 있어도 캐시가 갱신되면 새 닉네임으로 나간다.")
    void preferCacheOverStalePrincipal() {
        given(memberReader.findMember(MEMBER_ID)).willReturn(member(OLD_NICKNAME));
        memberProfiles.refresh(member(NEW_NICKNAME));

        // 소켓은 닉네임을 바꾸기 전에 열렸으므로 프린시펄에는 옛 닉네임이 박혀 있다.
        chatController.chat(ROOM_ID, principalWith(OLD_NICKNAME), new ChatRequest("안녕"));

        assertThat(capturedPayload()).containsEntry("nickname", NEW_NICKNAME);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedPayload() {
        ArgumentCaptor<ApiResponse<Object>> captor = ArgumentCaptor.forClass(ApiResponse.class);
        verify(broadcaster).send(eq(StompDestination.room(ROOM_ID)), captor.capture());

        return (Map<String, Object>) captor.getValue().getData();
    }

    private static MemberAdapter principalWith(String nickname) {
        return new MemberAdapter(member(nickname));
    }

    private static Member member(String nickname) {
        return MemberFixture.withId(MEMBER_ID, nickname);
    }
}
