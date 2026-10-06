package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fungame.songquiz.api.controller.response.ApiResponse;
import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.MemberProfileCache;
import com.fungame.songquiz.domain.member.MemberReader;
import com.fungame.songquiz.domain.room.ChatMessageEvent;
import com.fungame.songquiz.support.MemberFixture;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

@DisplayName("채팅에 실리는 닉네임은 소켓을 연 시점이 아니라 지금 값이다")
class ChatNotifierTest {

    private static final Long ROOM_ID = 7L;
    private static final Long MEMBER_ID = 1L;
    private static final String OLD_NICKNAME = "반달";
    private static final String NEW_NICKNAME = "보름달";

    private final StompBroadcaster broadcaster = mock(StompBroadcaster.class);
    private final MemberReader memberReader = mock(MemberReader.class);
    private final CacheManager cacheManager = new ConcurrentMapCacheManager(MemberProfileCache.CACHE_NAME);
    private final MemberProfileCache memberProfileCache = new MemberProfileCache(memberReader, cacheManager);
    private final ChatNotifier chatNotifier = new ChatNotifier(broadcaster, memberProfileCache);

    @Test
    @DisplayName("닉네임을 캐시에서 읽어 싣는다.")
    void carryNicknameFromCache() {
        given(memberReader.findMember(MEMBER_ID)).willReturn(member(OLD_NICKNAME));

        chatNotifier.handleChatMessage(new ChatMessageEvent(ROOM_ID, MEMBER_ID, "안녕"));

        assertThat(capturedPayload()).containsEntry("memberId", MEMBER_ID)
                .containsEntry("nickname", OLD_NICKNAME)
                .containsEntry("message", "안녕");
    }

    @Test
    @DisplayName("이벤트를 올린 시점의 닉네임이 아니라 캐시가 갱신된 새 닉네임으로 나간다.")
    void preferCacheOverStaleNickname() {
        given(memberReader.findMember(MEMBER_ID)).willReturn(member(OLD_NICKNAME));
        memberProfileCache.refresh(member(NEW_NICKNAME));

        chatNotifier.handleChatMessage(new ChatMessageEvent(ROOM_ID, MEMBER_ID, "안녕"));

        assertThat(capturedPayload()).containsEntry("nickname", NEW_NICKNAME);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedPayload() {
        ArgumentCaptor<ApiResponse<Object>> captor = ArgumentCaptor.forClass(ApiResponse.class);
        verify(broadcaster).send(eq(StompDestination.room(ROOM_ID)), captor.capture());

        return (Map<String, Object>) captor.getValue().getData();
    }

    private static Member member(String nickname) {
        return MemberFixture.withId(MEMBER_ID, nickname);
    }
}
