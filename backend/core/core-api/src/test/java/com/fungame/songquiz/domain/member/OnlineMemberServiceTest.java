package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.MemberLocation;
import com.fungame.songquiz.domain.room.MemberLocations;
import com.fungame.songquiz.enums.PlayerStatus;
import com.fungame.songquiz.support.MemberFixture;
import com.fungame.songquiz.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OnlineMemberServiceTest {

    private static final Long VIEWER_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long ROOM_ID = 7L;

    private final MemberConnectionTracker memberConnectionTracker = new MemberConnectionTracker(
            event -> {
            },
            new MutableClock(Instant.parse("2026-08-14T00:00:00Z"), ZoneId.of("UTC")));
    private final MemberReader memberReader = mock(MemberReader.class);
    private final GameRoomService gameRoomService = mock(GameRoomService.class);
    private final OnlineMemberService onlineMemberService =
            new OnlineMemberService(memberConnectionTracker, memberReader, gameRoomService);

    @BeforeEach
    void everyoneIsInLobbyByDefault() {
        given(gameRoomService.findEveryLocation()).willReturn(new MemberLocations(Map.of()));
    }

    @Test
    @DisplayName("접속 중인 회원을 모두 모아준다.")
    void findAllOnline() {
        connect(VIEWER_ID, OTHER_ID);
        given(memberReader.findAllInOrderByNickname(any())).willReturn(List.of(member(VIEWER_ID), member(OTHER_ID)));

        OnlineMembers onlineMembers = onlineMemberService.findAllOnline();

        assertThat(onlineMembers.members()).extracting(OnlineMemberInfo::memberId)
                .containsExactlyInAnyOrder(VIEWER_ID, OTHER_ID);
    }

    @Test
    @DisplayName("보는 사람 자신은 접속 중인 목록에서 뺀다.")
    void excludeViewerSelf() {
        connect(VIEWER_ID, OTHER_ID);
        given(memberReader.findAllInOrderByNickname(any())).willReturn(List.of(member(VIEWER_ID), member(OTHER_ID)));

        List<OnlineMemberInfo> others = onlineMemberService.findOthersOnline(VIEWER_ID);

        assertThat(others).extracting(OnlineMemberInfo::memberId).containsExactly(OTHER_ID);
    }

    @Test
    @DisplayName("한 번 모은 목록은 여러 사람의 화면을 만드는 데 다시 쓴다.")
    void reuseOneLookupForEveryViewer() {
        connect(VIEWER_ID, OTHER_ID);
        given(memberReader.findAllInOrderByNickname(any())).willReturn(List.of(member(VIEWER_ID), member(OTHER_ID)));

        OnlineMembers onlineMembers = onlineMemberService.findAllOnline();

        assertThat(onlineMembers.excluding(VIEWER_ID)).extracting(OnlineMemberInfo::memberId).containsExactly(OTHER_ID);
        assertThat(onlineMembers.excluding(OTHER_ID)).extracting(OnlineMemberInfo::memberId).containsExactly(VIEWER_ID);
        verify(memberReader, times(1)).findAllInOrderByNickname(any());
    }

    @Test
    @DisplayName("방에 있는 사람의 상태와 방 번호는 방 소속에서 가져온다.")
    void takeStatusFromRoomMembership() {
        connect(VIEWER_ID, OTHER_ID);
        given(memberReader.findAllInOrderByNickname(any())).willReturn(List.of(member(VIEWER_ID), member(OTHER_ID)));
        given(gameRoomService.findEveryLocation()).willReturn(new MemberLocations(
                Map.of(OTHER_ID, new MemberLocation(PlayerStatus.PLAYING, ROOM_ID))));

        OnlineMembers onlineMembers = onlineMemberService.findAllOnline();

        assertThat(onlineMembers.members())
                .extracting(OnlineMemberInfo::memberId, OnlineMemberInfo::status, OnlineMemberInfo::currentRoomId)
                .containsExactlyInAnyOrder(
                        tuple(VIEWER_ID, PlayerStatus.LOBBY, null),
                        tuple(OTHER_ID, PlayerStatus.PLAYING, ROOM_ID));
    }

    private void connect(Long... memberIds) {
        for (Long memberId : memberIds) {
            memberConnectionTracker.connect(memberId, "connection-" + memberId);
        }
    }

    private static Member member(Long memberId) {
        return MemberFixture.withId(memberId, "회원" + memberId);
    }
}
