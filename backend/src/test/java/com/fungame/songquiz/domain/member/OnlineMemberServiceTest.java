package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.dto.OnlineMemberInfo;
import com.fungame.songquiz.domain.dto.OnlineMembers;
import com.fungame.songquiz.support.MemberFixture;
import com.fungame.songquiz.support.MutableClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OnlineMemberServiceTest {

    private static final Long VIEWER_ID = 1L;
    private static final Long OTHER_ID = 2L;

    private final MemberConnectionTracker memberConnectionTracker = new MemberConnectionTracker(
            event -> {
            },
            new MutableClock(Instant.parse("2026-08-14T00:00:00Z"), ZoneId.of("UTC")));
    private final MemberPresenceService memberPresenceService = mock(MemberPresenceService.class);
    private final OnlineMemberService onlineMemberService =
            new OnlineMemberService(memberConnectionTracker, memberPresenceService);

    @Test
    @DisplayName("접속 중인 회원을 모두 모아준다.")
    void findAllOnline() {
        connect(VIEWER_ID, OTHER_ID);
        given(memberPresenceService.findAllIn(any())).willReturn(List.of(member(VIEWER_ID), member(OTHER_ID)));

        OnlineMembers onlineMembers = onlineMemberService.findAllOnline();

        assertThat(onlineMembers.members()).extracting(OnlineMemberInfo::memberId)
                .containsExactlyInAnyOrder(VIEWER_ID, OTHER_ID);
    }

    @Test
    @DisplayName("보는 사람 자신은 접속 중인 목록에서 뺀다.")
    void excludeViewerSelf() {
        connect(VIEWER_ID, OTHER_ID);
        given(memberPresenceService.findAllIn(any())).willReturn(List.of(member(VIEWER_ID), member(OTHER_ID)));

        List<OnlineMemberInfo> others = onlineMemberService.findOthersOnline(VIEWER_ID);

        assertThat(others).extracting(OnlineMemberInfo::memberId).containsExactly(OTHER_ID);
    }

    @Test
    @DisplayName("한 번 모은 목록은 여러 사람의 화면을 만드는 데 다시 쓴다.")
    void reuseOneLookupForEveryViewer() {
        connect(VIEWER_ID, OTHER_ID);
        given(memberPresenceService.findAllIn(any())).willReturn(List.of(member(VIEWER_ID), member(OTHER_ID)));

        OnlineMembers onlineMembers = onlineMemberService.findAllOnline();

        assertThat(onlineMembers.excluding(VIEWER_ID)).extracting(OnlineMemberInfo::memberId).containsExactly(OTHER_ID);
        assertThat(onlineMembers.excluding(OTHER_ID)).extracting(OnlineMemberInfo::memberId).containsExactly(VIEWER_ID);
        verify(memberPresenceService, times(1)).findAllIn(any());
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
